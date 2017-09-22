package org.opendaylight.cyy.listener;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.TestExec;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class DsbenchmarkcyyListener implements DataTreeChangeListener<TestExec>{
    private static final Logger LOG = LoggerFactory.getLogger(DsbenchmarkcyyListener.class);
    private AtomicInteger numEvents=new AtomicInteger(0);
    private AtomicInteger numDataChanges = new AtomicInteger(0);


    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<TestExec>> changes){
        //事件发生次数
        final int eventNum = numEvents.incrementAndGet();
        //有多少变更
        numDataChanges.addAndGet(changes.size());
        if(LOG.isDebugEnabled()){
            logDataTreeChangeEvent(eventNum,changes);
        }
    }

    private static synchronized void logDataTreeChangeEvent(final int eventNum,
                                                            final Collection<DataTreeModification<TestExec>> changes)
    {
        LOG.debug("DsbenchmarkcyyListner-onDataTreeChanged: Event{}",eventNum);

        for (DataTreeModification<TestExec> change : changes){
            final DataObjectModification<TestExec> rootNode = change.getRootNode();
            final DataObjectModification.ModificationType modType=rootNode.getModificationType();
            final InstanceIdentifier.PathArgument changeId=rootNode.getIdentifier();
            final Collection<DataObjectModification<? extends DataObject>> modifications =
                    rootNode.getModifiedChildren();

            LOG.debug("cyychangeId {}, modType {}, mods: {}", changeId, modType, modifications.size());

            for (DataObjectModification<? extends DataObject> mod : modifications){
                LOG.debug("cyymod-getDataAfter: {}",mod.getDataAfter());
            }

        }
    }

    public int getNumEvents() {return numEvents.get();}

    public int getNumDataChanges(){return numDataChanges.get();}
}
