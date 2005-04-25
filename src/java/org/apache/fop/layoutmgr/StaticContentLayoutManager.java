/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */
 
package org.apache.fop.layoutmgr;

import org.apache.fop.area.RegionReference;
import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.fo.pagination.Region;
import org.apache.fop.fo.pagination.SideRegion;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.traits.MinOptMax;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * LayoutManager for an fo:flow object.
 * Its parent LM is the PageSequenceLayoutManager.
 * This LM is responsible for getting columns of the appropriate size
 * and filling them with block-level areas generated by its children.
 */
public class StaticContentLayoutManager extends BlockStackingLayoutManager {
    private RegionReference targetRegion;
    private List blockBreaks = new ArrayList();

    public StaticContentLayoutManager(StaticContent node) {
        super(node);
    }

    /**
     * Sets the region reference
     * @param region region reference
     */
    public void setTargetRegion(RegionReference targetRegion) {
        this.targetRegion = targetRegion;
    }

    /** 
     * @return the region-reference-area that this 
     * static content is directed to.  
     */
    public RegionReference getTargetRegion() {
        return targetRegion;
    }
    
    /**
     * @see org.apache.fop.layoutmgr.LayoutManager#getNextKnuthElements(org.apache.fop.layoutmgr.LayoutContext, int)
     */
    public LinkedList getNextKnuthElements(LayoutContext context, int alignment) {
        // set layout dimensions
        fobj.setLayoutDimension(PercentBase.BLOCK_IPD, context.getRefIPD());
        fobj.setLayoutDimension(PercentBase.BLOCK_BPD, context.getStackLimit().opt);

        //TODO Copied from elsewhere. May be worthwhile to factor out the common parts. 
        // currently active LM
        BlockLevelLayoutManager curLM;
        BlockLevelLayoutManager prevLM = null;
        MinOptMax stackSize = new MinOptMax();
        LinkedList returnedList;
        LinkedList returnList = new LinkedList();

        while ((curLM = ((BlockLevelLayoutManager) getChildLM())) != null) {
            if (curLM.generatesInlineAreas()) {
                log.error("inline area not allowed under flow - ignoring");
                curLM.setFinished(true);
                continue;
            }

            // Set up a LayoutContext
            MinOptMax bpd = context.getStackLimit();
            BreakPoss bp;
            bp = null;

            LayoutContext childLC = new LayoutContext(0);
            boolean breakPage = false;
            childLC.setStackLimit(MinOptMax.subtract(bpd, stackSize));
            childLC.setRefIPD(context.getRefIPD());

            // get elements from curLM
            returnedList = curLM.getNextKnuthElements(childLC, alignment);
/*LF*/      //System.out.println("FLM.getNextKnuthElements> returnedList.size() = " + returnedList.size());

            // "wrap" the Position inside each element
            LinkedList tempList = returnedList;
            KnuthElement tempElement;
            returnedList = new LinkedList();
            ListIterator listIter = tempList.listIterator();
            while (listIter.hasNext()) {
                tempElement = (KnuthElement)listIter.next();
                tempElement.setPosition(new NonLeafPosition(this, tempElement.getPosition()));
                returnedList.add(tempElement);
            }

            if (returnedList.size() == 1
                && ((KnuthElement)returnedList.getFirst()).isPenalty()
                && ((KnuthPenalty)returnedList.getFirst()).getP() == -KnuthElement.INFINITE) {
                // a descendant of this flow has break-before
                returnList.addAll(returnedList);
                return returnList;
            } else {
                if (returnList.size() > 0) {
                    // there is a block before this one
                    if (prevLM.mustKeepWithNext()
                        || curLM.mustKeepWithPrevious()) {
                        // add an infinite penalty to forbid a break between blocks
                        returnList.add(new KnuthPenalty(0, KnuthElement.INFINITE, false, new Position(this), false));
                    } else if (!((KnuthElement) returnList.getLast()).isGlue()) {
                        // add a null penalty to allow a break between blocks
                        returnList.add(new KnuthPenalty(0, 0, false, new Position(this), false));
                    }
                }
/*LF*/          if (returnedList.size() > 0) { // controllare!
                    returnList.addAll(returnedList);
                    if (((KnuthElement)returnedList.getLast()).isPenalty()
                        && ((KnuthPenalty)returnedList.getLast()).getP() == -KnuthElement.INFINITE) {
                        // a descendant of this flow has break-after
/*LF*/                  //System.out.println("FLM - break after!!");
                        return returnList;
                    }
/*LF*/          }
            }
            prevLM = curLM;
        }

        setFinished(true);

        if (returnList.size() > 0) {
            return returnList;
        } else {
            return null;
        }
    }
    
    /**
     * @see org.apache.fop.layoutmgr.LayoutManager#getNextBreakPoss(LayoutContext)
     */
    public BreakPoss getNextBreakPoss(LayoutContext context) {

        // currently active LM
        LayoutManager curLM;

        while ((curLM = getChildLM()) != null) {
            // Make break positions and return page break
            // Set up a LayoutContext
            BreakPoss bp;
            LayoutContext childLC = context;
            if (!curLM.isFinished()) {
                if ((bp = curLM.getNextBreakPoss(childLC)) != null) {
                    blockBreaks.add(bp);
                    if (bp.isForcedBreak()) {
                        log.error("Forced breaks are not allowed in "
                                + "static content - ignoring");
                        return null;
                    }
                }
            }
        }
        setFinished(true);
        if (blockBreaks.size() > 0) {
            return new BreakPoss(
              new LeafPosition(this, blockBreaks.size() - 1));
        }
        return null;
    }

    /**
     * @see org.apache.fop.layoutmgr.LayoutManager#addAreas(PositionIterator, LayoutContext)
     */
    public void addAreas(PositionIterator parentIter, LayoutContext layoutContext) {
        AreaAdditionUtil.addAreas(parentIter, layoutContext);

        /*
        LayoutManager childLM;
        int iStartPos = 0;
        LayoutContext lc = new LayoutContext(0);
        while (parentIter.hasNext()) {
            LeafPosition lfp = (LeafPosition) parentIter.next();
            // Add the block areas to Area
            PositionIterator breakPosIter =
              new BreakPossPosIter(blockBreaks, iStartPos,
                                   lfp.getLeafPos() + 1);
            iStartPos = lfp.getLeafPos() + 1;
            while ((childLM = breakPosIter.getNextChildLM()) != null) {
                childLM.addAreas(breakPosIter, lc);
            }
        }

        blockBreaks.clear();
        */
        flush();
        targetRegion = null;
    }


    /**
     * Add child area to a the correct container, depending on its
     * area class. A Flow can fill at most one area container of any class
     * at any one time. The actual work is done by BlockStackingLM.
     * @see org.apache.fop.layoutmgr.LayoutManager#addChildArea(Area)
     */
    public void addChildArea(Area childArea) {
        targetRegion.addBlock((Block)childArea);
    }

    /**
     * @see org.apache.fop.layoutmgr.LayoutManager#getParentArea(Area)
     */
    public Area getParentArea(Area childArea) {
        return targetRegion;
    }

    public void doLayout(SideRegion region) {
        MinOptMax range = new MinOptMax(targetRegion.getIPD());
        StaticContentBreaker breaker = new StaticContentBreaker(region, this, range);
        breaker.doLayout(targetRegion.getBPD());
        if (breaker.isOverflow()) {
            if (region.getOverflow() == EN_ERROR_IF_OVERFLOW) {
                //TODO throw layout exception
            }
            log.warn("static-content overflows the available area.");
        }
    }
    
    private class StaticContentBreaker extends AbstractBreaker {
        
        private Region region;
        private StaticContentLayoutManager lm;
        private MinOptMax ipd;
        boolean overflow = false;
        
        public StaticContentBreaker(Region region, StaticContentLayoutManager lm, MinOptMax ipd) {
            this.region = region;
            this.lm = lm;
            this.ipd = ipd;
        }

        public boolean isOverflow() {
            return this.overflow;
        }
        
        protected LayoutManager getTopLevelLM() {
            return lm;
        }

        protected LayoutContext createLayoutContext() {
            LayoutContext lc = super.createLayoutContext();
            lc.setRefIPD(ipd.opt);
            return lc;
        }
        
        protected LinkedList getNextKnuthElements(LayoutContext context, int alignment) {
            LayoutManager curLM; // currently active LM
            LinkedList returnList = new LinkedList();

            while ((curLM = getChildLM()) != null) {
                LayoutContext childLC = new LayoutContext(0);
                childLC.setStackLimit(context.getStackLimit());
                childLC.setRefIPD(context.getRefIPD());

                LinkedList returnedList = null;
                if (!curLM.isFinished()) {
                    returnedList = curLM.getNextKnuthElements(childLC, alignment);
                }
                if (returnedList != null) {
                    lm.wrapPositionElements(returnedList, returnList);
                    //returnList.addAll(returnedList);
                }
            }
            setFinished(true);
            return returnList;
        }

        protected int getCurrentDisplayAlign() {
            return region.getDisplayAlign();
        }
        
        protected boolean hasMoreContent() {
            return !lm.isFinished();
        }
        
        protected void addAreas(PositionIterator posIter, LayoutContext context) {
            AreaAdditionUtil.addAreas(posIter, context);    
        }
        
        protected void doPhase3(PageBreakingAlgorithm alg, int partCount, 
                BlockSequence originalList, BlockSequence effectiveList) {
            //Directly add areas after finding the breaks
            addAreas(alg, partCount, originalList, effectiveList);
            if (partCount > 1) {
                overflow = true;
            }
        }
        
        protected void finishPart() {
            //nop for static content
        }
        
        protected LayoutManager getCurrentChildLM() {
            return null; //TODO NYI
        }
        
    }    
}

