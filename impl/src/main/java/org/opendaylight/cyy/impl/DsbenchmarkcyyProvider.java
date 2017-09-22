/*
 * Copyright © 2017 CYY, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.cyy.impl;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitDeadlockException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.cyy.listener.DsbenchmarkcyyListener;
import org.opendaylight.cyy.listener.DsbenchmarkcyyListenerProvider;
import org.opendaylight.cyy.simpletx.*;
//import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class DsbenchmarkcyyProvider implements DsbenchmarkcyyService{

    private static final Logger LOG = LoggerFactory.getLogger(DsbenchmarkcyyProvider.class);
    private static final InstanceIdentifier<TestExec> TEST_EXEC_IID =
            InstanceIdentifier.builder(TestExec.class).build();
    private static final InstanceIdentifier<TestStatus> TEST_STATUS_IID =
            InstanceIdentifier.builder(TestStatus.class).build();

    private final AtomicReference<TestStatus.ExecStatus> execStatus =
            new AtomicReference<>(TestStatus.ExecStatus.Idle);
    private long testsCompleted = 0;
    private final DataBroker simpleTxDataBroker;
    private final DOMDataBroker domDataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private DsbenchmarkcyyListenerProvider dsbenchmarkcyyListenerProvider = new DsbenchmarkcyyListenerProvider();

    public DsbenchmarkcyyProvider(final DOMDataBroker domDataBroker,final DataBroker simpleTxDataBroker,RpcProviderRegistry rpcProviderRegistry) {
        this.domDataBroker = domDataBroker;
        this.simpleTxDataBroker = simpleTxDataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("DsbenchmarkcyyProvider Session Initiated");

        dsbenchmarkcyyListenerProvider.setDataBroker(simpleTxDataBroker);
        rpcProviderRegistry.addRpcImplementation(DsbenchmarkcyyService.class,
                new DsbenchmarkcyyProvider(domDataBroker,simpleTxDataBroker,rpcProviderRegistry));
        //setTestOperData(execStatus.get(),testsCompleted); //由自己设定,为什么这里还要设定?不是自己输入吗
        LOG.info("cyy end test");
    }

    private void setTestOperData(final TestStatus.ExecStatus sts,final long tstCompl){//该函数用于测试写入数据库
        TestStatus status = new TestStatusBuilder().setExecStatus(sts)
                .setTestsCompleted(tstCompl)
                .build();
        WriteTransaction tx = simpleTxDataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL,TEST_STATUS_IID,status);

        try{
            tx.submit().checkedGet();
        }catch(final TransactionCommitFailedException e){
            throw new IllegalStateException(e);
        }

        LOG.info("cyy Datastore test oper status populated: {}" , status);
    }

        /*
    start a new data store write test run
     */
    public Future<RpcResult<StartTestOutput>> startTest(StartTestInput input){//input的是OPTIONAL ,两个数据库都有了
        final DatastoreAbstractWriter datastoreAbstractWriter;
        long startTime, endTime,listCreateTime,execTime;
        //Create listeners on OPERATIONAL and CONFIG test data subtrees
        dsbenchmarkcyyListenerProvider.setDataBroker(simpleTxDataBroker);
        dsbenchmarkcyyListenerProvider.createAndRegisterListeners(input.getListeners().intValue());
        datastoreAbstractWriter = getDatastoreWriter(input);

        cleanupStore();

        startTime = System.nanoTime();
        datastoreAbstractWriter.createList();
        endTime=System.nanoTime();
        listCreateTime = (endTime - startTime)/1000;
        try{
            startTime = System.nanoTime();
            datastoreAbstractWriter.executeList();
            endTime=System.nanoTime();
            execTime=(endTime-startTime)/1000;

            this.testsCompleted ++;
        }catch (final Exception e) {
            LOG.error("Test error: {}",e.toString());
            //execStatus.set(TestStatus.ExecStatus.Idle);
            return RpcResultBuilder.success(new StartTestOutputBuilder()
                    .setStatus(StartTestOutput.Status.FAILED)
                    .build()).buildFuture();
        }
        //如果成功执行了excuteList
        LOG.info("cyy Test finished");
        //setTestOperData(TestStatus.ExecStatus.Idle,testsCompleted);
        //execStatus.set(TestStatus.ExecStatus.Idle);

        long numDataChanges = dsbenchmarkcyyListenerProvider.getDataChangeCount();
        long numEvents = dsbenchmarkcyyListenerProvider.getEventCountAndDestroyListeners();

        StartTestOutput output = new StartTestOutputBuilder()
                .setStatus(StartTestOutput.Status.OK)
                .setListBuildTime(listCreateTime)
                .setExecTime(execTime)
                .setTxOk((long)datastoreAbstractWriter.txOk)
                .setTxError((long)datastoreAbstractWriter.txError)
                .setNtfOk(numEvents)
                .setDataChangeEventsOk(numDataChanges)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    public DatastoreAbstractWriter getDatastoreWriter(final StartTestInput input){//从这个函数的条件判断可以知道从上至下可以分为
        //txType: simple  chain
        //dataformat: BA  BI
        //Operation: read delete write
        //datastore:OPERATIONAL  CONFIGURATION
        final DatastoreAbstractWriter datastoreAbstractWriter;
        StartTestInput.Operation oper = input.getOperation();
        StartTestInput.DataFormat dataFormat = input.getDataFormat();
        int outerListElem = input.getOuterElements().intValue();//
        int innerListElem = input.getInnerElements().intValue();//
        long writesPerTx = input.getPutsPerTx().intValue();
        StartTestInput.DataStore enumDataStore = input.getDataStore();
        //在SimpletxBaWrite中重用了executeLISThecreatelist函数
        if (dataFormat == StartTestInput.DataFormat.BINDINGAWARE){
            if(oper == StartTestInput.Operation.DELETE){
                datastoreAbstractWriter = new SimpletxBaDelete(
                        simpleTxDataBroker,outerListElem,innerListElem,writesPerTx,enumDataStore);
            }else if(oper == StartTestInput.Operation.READ){
                datastoreAbstractWriter=new SimpletxBaDelete(
                        simpleTxDataBroker,outerListElem,innerListElem,writesPerTx,enumDataStore);
            }else{
                datastoreAbstractWriter=new SimpletxBaWrite(
                        simpleTxDataBroker,oper,outerListElem,innerListElem,writesPerTx,enumDataStore
                );
            }

        }else{
            if(oper == StartTestInput.Operation.DELETE){
                datastoreAbstractWriter = new SimpletxDomDelete(
                        this.domDataBroker,outerListElem,innerListElem,writesPerTx,enumDataStore);
            }else if(oper == StartTestInput.Operation.READ){
                datastoreAbstractWriter=new SimpletxDomRead(
                        domDataBroker,outerListElem,innerListElem,writesPerTx,enumDataStore);
            }else{
                datastoreAbstractWriter=new SimpletxDomWrite(
                        domDataBroker,oper,outerListElem,innerListElem,writesPerTx,enumDataStore);
            }

        }
        //execStatus.set(TestStatus.ExecStatus.Idle);
        return datastoreAbstractWriter;
    }
    /**
     * Delete data in the test-exec container that may have been left behind from a
     * previous test run
     *
     */
    public Future<RpcResult<Void>> cleanupStore(){
        WriteTransaction tx = simpleTxDataBroker.newWriteOnlyTransaction();
        //给exec写入空数据,代替删除
        TestExec testExec = new TestExecBuilder().setOuterList(Collections.emptyList()).build();
        tx.put(LogicalDatastoreType.OPERATIONAL,TEST_EXEC_IID,testExec);

        try{
            tx.submit().checkedGet();
            LOG.debug("DataStore OPERATIONAL test data cleaned up");
        }catch (final TransactionCommitFailedException e){
            LOG.info("Failed to cleanup DataStore configtest data");
            throw new IllegalStateException(e);
        } //只给OPERATIONAL添加了空数据,是不是线程的问题
        tx = simpleTxDataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION,TEST_EXEC_IID,testExec);
        try{
            tx.submit().checkedGet();
            LOG.debug("DataStore config test data cleaned up");
        }catch (final TransactionCommitFailedException e){
            LOG.info("Failed to cleanup DataStore configtest data");
            throw new IllegalStateException(e);
        }


        //测试写入status

        TestStatus status = new TestStatusBuilder()
                .setExecStatus(execStatus.get())
                .setTestsCompleted((long)200)
                .build();
        tx = simpleTxDataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL,TEST_STATUS_IID,status);

        try{
            tx.submit().checkedGet();
        } catch (final TransactionCommitFailedException e){
            throw new IllegalStateException(e);
        }
//        TestStatus status1 = new TestStatusBuilder()
//                .setExecStatus(execStatus.get())
//                .setTestsCompleted((long)200)
//                .build();
//        tx = simpleTxDataBroker.newWriteOnlyTransaction();
//        tx.put(LogicalDatastoreType.CONFIGURATION,TEST_STATUS_IID,status1);
//在yang中设置status是无法写入config的.相当于是存在CONFIG 和OPERATIONAL 两种逻辑上的数据库,有status和exec两种数据种类,内容都在yang中有规定
//        try{
//            tx.submit().checkedGet();
//        } catch (final TransactionCommitFailedException e){
//            throw new IllegalStateException(e);
//        }



//        TestStatus status = new TestStatusBuilder()
//                .setExecStatus(execStatus.get())
//                .setTestsCompleted((long)201)
//                .build();
//        tx = simpleTxDataBroker.newWriteOnlyTransaction();
//        tx.put(LogicalDatastoreType.CONFIGURATION,TEST_STATUS_IID,status);
//
//        try{
//            tx.submit().checkedGet();
//        } catch (final TransactionCommitFailedException e){
//            throw new IllegalStateException(e);
//        }
//
        LOG.info("cyy Datastore test oper status populated:{}",status);
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("DsbenchmarkcyyProvider Closed");
    }



}