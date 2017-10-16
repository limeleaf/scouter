/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouterx.webapp.layer.consumer;

import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.model.scouter.SCounter;
import scouterx.webapp.request.CounterRequestByType;
import scouterx.webapp.view.CounterView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class CounterConsumer {

    /**
     * get realtime counters's values by type
     * @param objType
     * @param counterNames
     * @param server
     * @return
     */
    public List<SCounter> retrieveRealTimeCountersByObjType(final String objType, Set<String> counterNames, final Server server) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.OBJ_TYPE, objType);
        ListValue counterNameLv = paramPack.newList(ParamConstant.COUNTER);
        for (String name : counterNames) {
            counterNameLv.add(name);
        }

        MapPack outMapPack;
        try(TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            outMapPack = (MapPack) tcpProxy.getSingle(RequestCmd.COUNTER_REAL_TIME_ALL_MULTI, paramPack);
        }

        ListValue rObjHashLv = (ListValue) outMapPack.get(ParamConstant.OBJ_HASH);
        ListValue rCounterNameLv = (ListValue) outMapPack.get(ParamConstant.COUNTER);
        ListValue rCounterValueLv = (ListValue) outMapPack.get(ParamConstant.VALUE);

        List<SCounter> resultList = new ArrayList<>();
        for(int i = 0; i < rObjHashLv.size(); i++) {
            int objHash = CastUtil.cint(rObjHashLv.get(i));
            String counterName = rCounterNameLv.getString(i);
            Object counterValue = rCounterValueLv.get(i).toJavaObject();

            resultList.add(new SCounter(objHash, counterName, counterValue));
        }
        return resultList;
    }

    /**
     * get daily counter (precision : 5 min avg) values by objType
     */
    public List<CounterView> retrieveCounterByObjType(CounterRequestByType request) {
        MapPack paramPack = new MapPack();
        paramPack.put(ParamConstant.OBJ_TYPE, request.getObjType());
        paramPack.put(ParamConstant.SDATE, request.getFromYmd());
        paramPack.put(ParamConstant.EDATE, request.getToYmd());
        paramPack.put(ParamConstant.COUNTER, request.getCounter());

        List<CounterView> counterViewList = new ArrayList<>();
        Server server = ServerManager.getInstance().getServerIfNullDefault(request.getServerId());

        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            tcpProxy.process(RequestCmd.COUNTER_PAST_LONGDATE_ALL, paramPack, in -> {
                MapPack mapPack = (MapPack) in.readPack();
                int objHash = mapPack.getInt(ParamConstant.OBJ_HASH);
                ListValue timeList = mapPack.getList(ParamConstant.TIME);
                ListValue valueList = mapPack.getList(ParamConstant.VALUE);

                List<Double> valueToDoubleList = new ArrayList<>();
                for (int i = 0; i < timeList.size(); i++) {
                    valueToDoubleList.add(valueList.getDouble(i));
                }

                CounterView counterView = CounterView.builder()
                        .objHash(objHash)
                        .name(request.getCounter())
                        .displayName(server.getCounterEngine().getCounterDisplayName(request.getObjType(), request.getCounter()))
                        .unit(server.getCounterEngine().getCounterUnit(request.getObjType(), request.getCounter()))
                        .fromYmd(request.getFromYmd())
                        .toYmd(request.getToYmd())
                        .timeList(Arrays.stream(timeList.toObjectArray()).map(Long.class::cast).collect(Collectors.toList()))
                        .valueList(valueToDoubleList)
                        .build();

                counterViewList.add(counterView);
            });
        }

        return counterViewList;
    }
}
