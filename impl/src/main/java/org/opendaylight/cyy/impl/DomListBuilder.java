package org.opendaylight.cyy.impl;

import javassist.runtime.Inner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmarkcyy.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;

import java.util.ArrayList;
import java.util.List;

public final class DomListBuilder {
    //DOM使用的是QName,QName已经在InnerList和 OuterList的class里面定义好了,由yang产生
    private static final org.opendaylight.yangtools.yang.common.QName IL_NAME = QName.create(InnerList.QNAME,"name");
    private static final org.opendaylight.yangtools.yang.common.QName IL_VALUE = QName.create(InnerList.QNAME,"value");

    private static final org.opendaylight.yangtools.yang.common.QName OL_ID = QName.create(OuterList.QNAME,"id");

    public static List<MapEntryNode> buildOuterList(final int outerElements, final int innerEllements)
    {
        List<MapEntryNode> outerList = new ArrayList<>(outerElements);
        for (int j =0;j<outerElements;j++){
            outerList.add(ImmutableNodes.mapEntryBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifierWithPredicates(OuterList.QNAME,OL_ID,j))
            .withChild(ImmutableNodes.leafNode(OL_ID,j))
            .withChild(buildInnerList(j,innerEllements))
            .build());
        }
        return outerList;
    }


    private static MapNode buildInnerList(final int index,final int elements) {
        CollectionNodeBuilder<MapEntryNode, MapNode> innerList = ImmutableNodes.mapNodeBuilder(InnerList.QNAME);

        final String itemStr = "Item-" + String.valueOf(index) + "-";
        for (int i = 0; i < elements; i++) {
            innerList.addChild(ImmutableNodes.mapEntryBuilder()
                    .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifierWithPredicates(InnerList.QNAME, IL_NAME, i))
                    .withChild(ImmutableNodes.leafNode(IL_NAME, i))
                    .withChild(ImmutableNodes.leafNode(IL_VALUE, itemStr + String.valueOf(i)))
                    .build());
        }
        return innerList.build();
    }

}
