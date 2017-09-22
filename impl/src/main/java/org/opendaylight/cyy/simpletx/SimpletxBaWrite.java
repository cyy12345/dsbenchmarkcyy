package org.opendaylight.cyy.simpletx;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.cyy.impl.BaListBuilder;
import org.opendaylight.cyy.impl.DatastoreAbstractWriter;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SimpletxBaWrite extends DatastoreAbstractWriter{
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxBaWrite.class);
    private final DataBroker dataBroker;
    private List<OuterList> outerLists;

    public SimpletxBaWrite(final DataBroker dataBroker, final StartTestInput.Operation oper,
                           final int outerListElem, final int innerListElem, final long writesPerTx,
                           final StartTestInput.DataStore dataStore){
        //super指向离自己最近的一个父类,与父类的成员变量同名
        super(oper,outerListElem,innerListElem,writesPerTx,dataStore);
        this.dataBroker = dataBroker;

    }

    @Override
    public void createList(){
        outerLists = BaListBuilder.buildOuterList(outerListElem,innerListElem);
    }
    @Override
    public void executeList(){
        LogicalDatastoreType logicalDatastoreType=getDataStoreType();//直接使用的DatastoreAbstractWriter中的函数,里面的datastore是input得到的
        WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
        long writeCnt = 0;

        for(OuterList outerList : outerLists){//遍历outerLists
            InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                    .child(OuterList.class,outerList.getKey());
            if(oper == StartTestInput.Operation.PUT){
                wt.put(logicalDatastoreType,iid,outerList);//logicalDatastoreType可能是config,operational
            }else{
                wt.merge(logicalDatastoreType,iid,outerList);
            }

            writeCnt++;

            if(writeCnt == writesPerTx) {//写入的次数已经达到了每次TX的写入次数
                try{
                    wt.submit().checkedGet();
                    txOk++;
                    LOG.info("cyy WriteTransaction write");
                }catch(TransactionCommitFailedException e){
                    LOG.error("cyy Transaction failed: {}",e);
                    txError++;
                }
                wt=dataBroker.newWriteOnlyTransaction();
                writeCnt=0;
            }
        }
        if(writeCnt!=0){
            try{
                wt.submit().checkedGet();
            }catch(final TransactionCommitFailedException e){
                LOG.error("cyy Transaction faield: {}",e);
            }
        }
    }
}
