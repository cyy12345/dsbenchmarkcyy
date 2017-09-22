package org.opendaylight.cyy.simpletx;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.cyy.impl.DatastoreAbstractWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SimpletxBaDelete extends DatastoreAbstractWriter{
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxBaDelete.class);
    private final DataBroker dataBroker;
    public SimpletxBaDelete(final DataBroker dataBroker, final int outerListElem, final int innerListElem, final long writesPerTx,
                            final StartTestInput.DataStore dataStore){
        super(StartTestInput.Operation.DELETE,outerListElem,innerListElem,writesPerTx,dataStore);
        this.dataBroker = dataBroker;

    }

    @Override
    public void createList(){
        //搞点数据进去，否则没得可删
        SimpletxBaWrite simpletxBaWrite = new SimpletxBaWrite(dataBroker,
                StartTestInput.Operation.PUT,
                outerListElem,
                innerListElem,
                outerListElem,
                dataStore);
        simpletxBaWrite.createList();
        simpletxBaWrite.executeList();

    }
    @Override
    public void executeList(){
        LogicalDatastoreType logicalDatastoreType = getDataStoreType();
        WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
        long writeCnt = 0;

        for(int i = 0; i < outerListElem; i ++){
            InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                    .child(OuterList.class, new OuterListKey(i));

            wt.delete(logicalDatastoreType, iid);

            writeCnt++;

            //以writesPerTx为单位，写入数据库。 writesPerTx相当于buffer
            if (writeCnt == writesPerTx) {
                try{
                    wt.submit().checkedGet();
                    txOk++;
                    LOG.info("cyy WriteTransaction write ");
                }catch(TransactionCommitFailedException e){
                    LOG.error("cyy Transaction failed: {}", e);
                    txError++;
                }
                wt = dataBroker.newWriteOnlyTransaction();
                writeCnt = 0;
            }

        }

        //将剩余的写入数据库
        if (writeCnt != 0){
            try{
                wt.submit().checkedGet();
            }catch(final TransactionCommitFailedException e){
                LOG.error("dzp Transaction failed: {}", e);
            }
        }


    }
}
