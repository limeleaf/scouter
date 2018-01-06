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

import scouter.lang.pack.ObjectPack;
import scouter.net.RequestCmd;
import scouterx.webapp.framework.client.net.TcpProxy;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.model.scouter.SObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 27.
 */
public class ObjectConsumer {

    /**
     * retrieve object(agent) list from collector server
     */
    public List<SObject> retrieveObjectList(final Server server) {
        List<SObject> objectList = null;
        try (TcpProxy tcpProxy = TcpProxy.getTcpProxy(server)) {
            objectList = tcpProxy
                    .process(RequestCmd.OBJECT_LIST_REAL_TIME, null).stream()
                    .map(p -> SObject.of((ObjectPack) p, server))
                    .collect(Collectors.toList());
        }

        return objectList;
    }
}
