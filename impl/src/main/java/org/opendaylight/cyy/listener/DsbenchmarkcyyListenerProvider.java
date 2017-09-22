package org.opendaylight.cyy.listener;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.TestExec;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;

public class DsbenchmarkcyyListenerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DsbenchmarkcyyListenerProvider.class);
    private static final InstanceIdentifier<TestExec> TEST_EXEC_IID =
            InstanceIdentifier.builder(TestExec.class).build();
    private final List<ListenerRegistration<DsbenchmarkcyyListener>> listeners =
            new ArrayList<>();
    private DataBroker dataBroker;

    public void setDataBroker(final DataBroker dataBroker){
        this.dataBroker = dataBroker;
        LOG.debug("DsbenchmarkcyyListenerProvider created");
    }

    public void createAndRegisterListeners(int numListeners){
        for (int i=0;i<numListeners;i++){
            DsbenchmarkcyyListener dsbenchmarkcyyListener = new DsbenchmarkcyyListener();
            dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,TEST_EXEC_IID),dsbenchmarkcyyListener);
            dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,TEST_EXEC_IID),dsbenchmarkcyyListener);
        }

        LOG.info("cyy createAndRegisterListeners");
    }

    public long getDataChangeCount(){
        long dataChanges = 0;

        for (ListenerRegistration<DsbenchmarkcyyListener> listenerRegistration : listeners){
            dataChanges += listenerRegistration.getInstance().getNumDataChanges();
        }
        LOG.debug("DsbenchmarkcyyLisenerProvider , total data changes {}", dataChanges);
        return dataChanges;
    }

    public long getEventCountAndDestroyListeners(){
        long totalEvents = 0;

        for (ListenerRegistration<DsbenchmarkcyyListener> listenerRegistration : listeners){
            totalEvents+=listenerRegistration.getInstance().getNumEvents();
            listenerRegistration.close();
        }
        listeners.clear();
        LOG.debug("DsbenchmarkListenerProvider destroyed listeners, total events {}",totalEvents);
        return totalEvents;
    }
}

