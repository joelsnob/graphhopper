/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.ch;

import com.graphhopper.routing.PathBidirRef;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.WeightCalculation;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeBase;
import com.graphhopper.util.EdgeSkipExplorer;

/**
 * Recursivly unpack shortcuts.
 * <p/>
 * @see PrepareContractionHierarchies
 * @author Peter Karich
 */
public class Path4CH extends PathBidirRef
{
    private WeightCalculation calc;

    public Path4CH( Graph g, FlagEncoder encoder, WeightCalculation calc )
    {
        super(g, encoder);
        this.calc = calc;
    }

    @Override
    protected void processDistance( int tmpEdge, int endNode )
    {
        EdgeBase mainIter = graph.getEdgeProps(tmpEdge, endNode);

        // Shortcuts do only contain valid weight so first expand before adding
        // to distance and time
        expandEdge((EdgeSkipExplorer) mainIter, false);
    }

    @Override
    public double calcDistance( EdgeBase mainIter )
    {
        return calc.revertWeight(mainIter.getDistance(), mainIter.getFlags());
    }

    private void expandEdge( EdgeSkipExplorer mainIter, boolean revert )
    {
        if (!mainIter.isShortcut())
        {
            double dist = calcDistance(mainIter);
            distance += dist;
            int flags = mainIter.getFlags();
            time += calcTime(dist, flags);
            addEdge(mainIter.getEdge());
            return;
        }

        int skippedEdge1 = mainIter.getSkippedEdge1();
        int skippedEdge2 = mainIter.getSkippedEdge2();
        int from = mainIter.getBaseNode(), to = mainIter.getAdjNode();
        if (revert)
        {
            int tmp = from;
            from = to;
            to = tmp;
        }

        // getEdgeProps could possibly return an empty edge if the shortcut is available for both directions
        if (reverseOrder)
        {
            EdgeSkipExplorer iter = (EdgeSkipExplorer) graph.getEdgeProps(skippedEdge1, to);
            boolean empty = iter == null;
            if (empty)
            {
                iter = (EdgeSkipExplorer) graph.getEdgeProps(skippedEdge2, to);
            }
            expandEdge(iter, false);

            if (empty)
            {
                iter = (EdgeSkipExplorer) graph.getEdgeProps(skippedEdge1, from);
            } else
            {
                iter = (EdgeSkipExplorer) graph.getEdgeProps(skippedEdge2, from);
            }
            expandEdge(iter, true);
        } else
        {
            EdgeSkipExplorer iter = (EdgeSkipExplorer) graph.getEdgeProps(skippedEdge1, from);
            boolean empty = iter == null;
            if (empty)
            {
                iter = (EdgeSkipExplorer) graph.getEdgeProps(skippedEdge2, from);
            }
            expandEdge(iter, true);

            if (empty)
            {
                iter = (EdgeSkipExplorer) graph.getEdgeProps(skippedEdge1, to);
            } else
            {
                iter = (EdgeSkipExplorer) graph.getEdgeProps(skippedEdge2, to);
            }
            expandEdge(iter, false);
        }
    }
}
