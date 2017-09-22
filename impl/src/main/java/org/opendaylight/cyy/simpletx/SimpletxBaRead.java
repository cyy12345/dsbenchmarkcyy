/*
read作用是读取数据,需要检查数据库是否为空,读出的数据放入一个数组中
 */

package org.opendaylight.cyy.simpletx;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.cyy.impl.DatastoreAbstractWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

public class SimpletxBaRead extends DatastoreAbstractWriter{
    private  static final Logger LOG = LoggerFactory.getLogger(SimpletxBaRead.class);
    private final DataBroker dataBroker;
    public SimpletxBaRead(final DataBroker dataBroker,
        final int outerListElem,final int innerListElem,final long writesPerTx,
                          final StartTestInput.DataStore dataStore){
        super(StartTestInput.Operation.READ,outerListElem,innerListElem,writesPerTx,dataStore);
        this.dataBroker=dataBroker;
    }

    @Override
    public void createList(){
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
    public void executeList()
    {
        LogicalDatastoreType logicalDatastoreType = getDataStoreType();
        ReadOnlyTransaction rt = dataBroker.newReadOnlyTransaction();

        for(int l=0;l<outerListElem;l++)
        {
            InstanceIdentifier<OuterList> iid_outerList = InstanceIdentifier.create(TestExec.class)
                    .child(OuterList.class,new OuterListKey(l));
            //用optional判断null,checkedFuture获得一个read线程
            CheckedFuture<Optional<OuterList>, ReadFailedException> checkedFuture =rt.read(logicalDatastoreType,iid_outerList);
            Optional<OuterList> optional_outerList;

            try{
                optional_outerList = checkedFuture.checkedGet();
                if (optional_outerList != null && optional_outerList.isPresent()){
                    OuterList outerList = optional_outerList.get();
                    //用数组装outerlist
                    String[] objectsArray = new String[outerList.getInnerList().size()];
                    for(InnerList innerList : outerList.getInnerList()){
                        //建一个数组，id是innterlist的name，数组的值是value
                        objectsArray[innerList.getName()]=innerList.getValue();
                    }
                }
            }catch(final ReadFailedException e){
                LOG.warn("failed to ...", e);
                txError++;
            }
        }
    }
}




