package org.opendaylight.cyy.impl;

//import org.opendaylight.mdsal.common.api.LogicalDatastoreType;注意引用不要错了
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.StartTestInput;

import java.util.Random;

/*
Author:cyy
 */
public abstract class DatastoreAbstractWriter {
    protected final int outerListElem;
    protected final int innerListElem;
    protected final long writesPerTx;
    protected final StartTestInput.Operation oper;
    protected final StartTestInput.DataStore dataStore;
    protected final Random rn = new Random();
    protected int txOk = 0;
    protected int txError =0;

    public DatastoreAbstractWriter(final StartTestInput.Operation oper,
                                   final int outerListElem, final int innerListElem,
                                   final long writesPerTx,final StartTestInput.DataStore dataStore){
        this.outerListElem=outerListElem;
        this.innerListElem=innerListElem;
        this.writesPerTx=writesPerTx;
        this.oper=oper;
        this.dataStore=dataStore;
    }

    public abstract void createList();

    public abstract void executeList();

    protected LogicalDatastoreType getDataStoreType(){
        final LogicalDatastoreType dsType;
        if (dataStore == StartTestInput.DataStore.CONFIG){
            dsType = LogicalDatastoreType.CONFIGURATION;
        }else if(dataStore==StartTestInput.DataStore.OPERATIONAL){
            dsType = LogicalDatastoreType.OPERATIONAL;
        }else{
            if (rn.nextBoolean()){
                dsType = LogicalDatastoreType.OPERATIONAL;
            }else{
                dsType = LogicalDatastoreType.CONFIGURATION;
            }
        }
        return dsType;
    }



}
