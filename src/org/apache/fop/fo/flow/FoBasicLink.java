/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 */

package org.apache.fop.fo.flow;

// FOP
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.PropertySets;
import org.apache.fop.fo.FObjectNames;
import org.apache.fop.fo.FObjects;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOTree;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.xml.FoXMLEvent;
import org.apache.fop.xml.UnexpectedStartElementException;
import org.apache.fop.apps.FOPException;
import org.apache.fop.datastructs.TreeException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.Ints;
import org.apache.fop.messaging.MessageHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.BitSet;

/**
 * Implements the fo:basic-link flow object.
 */
public class FoBasicLink extends FONode {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /** Map of <tt>Integer</tt> indices of <i>sparsePropsSet</i> array.
        It is indexed by the FO index of the FO associated with a given
        position in the <i>sparsePropsSet</i> array. See
        {@link org.apache.fop.fo.FONode#sparsePropsSet FONode.sparsePropsSet}.
     */
    private static final int[] sparsePropsMap;

    /** An <tt>int</tt> array of of the applicable property indices, in
        property index order. */
    private static final int[] sparseIndices;

    /** The number of applicable properties.  This is the size of the
        <i>sparsePropsSet</i> array. */
    private static final int numProps;

    static {
        // Collect the sets of properties that apply
        BitSet propsets = new BitSet();
        propsets.or(PropertySets.accessibilitySet);
        propsets.or(PropertySets.auralSet);
        propsets.or(PropertySets.backgroundSet);
        propsets.or(PropertySets.borderSet);
        propsets.or(PropertySets.paddingSet);
        propsets.or(PropertySets.marginInlineSet);
        propsets.or(PropertySets.relativePositionSet);
        propsets.set(PropNames.ALIGNMENT_ADJUST);
        propsets.set(PropNames.ALIGNMENT_BASELINE);
        propsets.set(PropNames.BASELINE_SHIFT);
        propsets.set(PropNames.DESTINATION_PLACEMENT_OFFSET);
        propsets.set(PropNames.DOMINANT_BASELINE);
        propsets.set(PropNames.EXTERNAL_DESTINATION);
        propsets.set(PropNames.ID);
        propsets.set(PropNames.INDICATE_DESTINATION);
        propsets.set(PropNames.INTERNAL_DESTINATION);
        propsets.set(PropNames.KEEP_TOGETHER);
        propsets.set(PropNames.KEEP_WITH_NEXT);
        propsets.set(PropNames.KEEP_WITH_PREVIOUS);
        propsets.set(PropNames.LINE_HEIGHT);
        propsets.set(PropNames.SHOW_DESTINATION);
        propsets.set(PropNames.TARGET_PROCESSING_CONTEXT);
        propsets.set(PropNames.TARGET_PRESENTATION_CONTEXT);
        propsets.set(PropNames.TARGET_STYLESHEET);

        // Map these properties into sparsePropsSet
        // sparsePropsSet is a HashMap containing the indicies of the
        // sparsePropsSet array, indexed by the FO index of the FO slot
        // in sparsePropsSet.
        sparsePropsMap = new int[PropNames.LAST_PROPERTY_INDEX + 1];
        Arrays.fill(sparsePropsMap, -1);
        numProps = propsets.cardinality();
        sparseIndices = new int[numProps];
        int propx = 0;
        for (int next = propsets.nextSetBit(0);
                next >= 0;
                next = propsets.nextSetBit(next + 1)) {
            sparseIndices[propx] = next;
            sparsePropsMap[next] = propx++;
        }
    }

    /**
     * Construct an fo:basic-link node, and build the
     * fo:basic-link subtree.
     * <p>Content model for fo:basic-link: (#PCDATA|%inline;|%block;)*
     * @param foTree the FO tree being built
     * @param parent the parent FONode of this node
     * @param event the <tt>FoXMLEvent</tt> that triggered the creation of
     * this node
     * @param stateFlags - passed down from the parent.  Includes the
     * attribute set information.
     */
    public FoBasicLink
            (FOTree foTree, FONode parent, FoXMLEvent event, int stateFlags)
        throws TreeException, FOPException
    {
        super(foTree, FObjectNames.BASIC_LINK, parent, event,
                          stateFlags, sparsePropsMap, sparseIndices);
        FoXMLEvent ev = null;
        do {
            try {
                if ((stateFlags & FONode.MC_OUT_OF_LINE) == 0)
                    ev = xmlevents.expectPcdataOrInlineOrBlock();
                else
                    ev = xmlevents.expectOutOfLinePcdataOrInlineOrBlock();
                if (ev != null) {
                    // Generate the flow object
                    //System.out.println("Generating flow object for " + ev);
                    FObjects.fobjects.makeFlowObject
                                (foTree, this, ev, stateFlags);
                    if (ev.getFoType() != FObjectNames.PCDATA)
                        ev = xmlevents.getEndElement(xmlevents.DISCARD_EV, ev);
                    pool.surrenderEvent(ev);
                }
            } catch(UnexpectedStartElementException e) {
                ev = xmlevents.getStartElement();
                MessageHandler.logln
                        ("Ignoring unexpected Start Element: "
                                                         + ev.getQName());
                ev = xmlevents.getEndElement(xmlevents.DISCARD_EV, ev);
                pool.surrenderEvent(ev);
            }
        } while (ev != null);

        makeSparsePropsSet();
    }

}
