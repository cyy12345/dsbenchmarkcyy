package org.opendaylight.cyy.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.outer.list.InnerListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.outer.list.InnerListKey;

import java.util.ArrayList;
import java.util.List;





public class BaListBuilder {
    public static List<OuterList> buildOuterList(final int outerElements, final int innerElements) {
        List<OuterList> outerLists = new ArrayList<>(outerElements);
        for(int i = 0;i < outerElements; i ++){
            outerLists.add(new OuterListBuilder()
                    .setId(i)
                    .setKey(new OuterListKey(i))
                    .setInnerList(buildInnerList(i, innerElements))
                    .build());
        }
        return outerLists;
    }


    private static List<InnerList> buildInnerList(final int index, final int elements ) {
        List<InnerList> innerLists = new ArrayList<>(elements);
        final String itemStr = "Item-" + String.valueOf(index) + "-";
        for(int i = 0; i < elements; i++){
            innerLists.add(new InnerListBuilder()
                    .setKey(new InnerListKey(i))
                    .setName(i)
                    .setValue(itemStr + String.valueOf(i))
                    .build());

        }
        return innerLists;
    }
}
