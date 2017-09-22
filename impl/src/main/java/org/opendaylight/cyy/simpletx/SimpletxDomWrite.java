package org.opendaylight.cyy.simpletx;


import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.cyy.impl.DatastoreAbstractWriter;
import org.opendaylight.cyy.impl.DomListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SimpletxDomWrite extends DatastoreAbstractWriter{

    private static final Logger LOG = LoggerFactory.getLogger(SimpletxDomWrite.class);
    private final DOMDataBroker domDataBroker;
    private List<MapEntryNode> list;


    public SimpletxDomWrite(final DOMDataBroker domDataBroker, final StartTestInput.Operation oper,
                            final int outerListElem,final int innerListElem, final long putsPerTx,
                            final StartTestInput.DataStore dataStore){
        super(oper,outerListElem,innerListElem,putsPerTx,dataStore);
        this.domDataBroker=domDataBroker;

    }
    @Override
    public void createList(){
        list = DomListBuilder.buildOuterList(this.outerListElem,this.innerListElem);
        //list里放的就是要写入的数据
    }
    @Override
    public void executeList(){
        final LogicalDatastoreType dsType = getDataStoreType();
        final YangInstanceIdentifier pid =
                YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();

        DOMDataWriteTransaction tx = domDataBroker.newWriteOnlyTransaction();
        long writeCnt = 0;

        for (MapEntryNode element :this.list){
            YangInstanceIdentifier yid =
                    pid.node(new YangInstanceIdentifier.NodeIdentifierWithPredicates(OuterList.QNAME,element.getIdentifier().getKeyValues()));
            //DOM产生标识符的过程还是复杂一些
            if(oper == StartTestInput.Operation.PUT){
                tx.put(dsType,yid,element);
            }else{
                tx.merge(dsType,yid,element);//DOM使用的是yid,BA使用的是iid
            }
            writeCnt++;

            if(writeCnt == writesPerTx){
                try {
                    tx.submit().checkedGet();  //都是先PUT或者MERGE后再submit,tx相当于寄存器或工作区
                    txOk++;
                } catch (TransactionCommitFailedException e) {
                    LOG.error("Transaction failed",e);
                    txError++;
                }
                tx = domDataBroker.newWriteOnlyTransaction();
                writeCnt = 0;
            }
        }

        if (writeCnt !=0){//将剩下的都submit了
            try {
                tx.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Transaction failed: {}",e);
            }
        }

    }
}
