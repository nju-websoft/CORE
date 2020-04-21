package name.sxli.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.SimpleGraph;

import name.dxliu.associations.AssociationTree;
import name.dxliu.bean.IntegerEdge;
import name.dxliu.util.TimeCounter;
import name.sxli.beans.Node;

/**
 * This util provide paths combination.
 *
 */
public class CombinationUtil {
	
	/**
	 * @param associationLimits
	 *            expected association number.
	 * @param timeCounter
	 *            time logger
	 * @param trees
	 *            association trees.
	 * @param canonicalCodeSet
	 *            tree's code that have been found.
	 * @param inputs
	 *            paths enumerated from different query entities.
	 * @param delta
	 *            diameter constraint.
	 * @param queryEntities
	 *            query entities.
	 * @return whether the enumeration is exhausted.
	 */
	public static boolean CombinateAndGenerateResult(int associationLimits, TimeCounter timeCounter,
			List<AssociationTree> result, Set<List<Integer>> canonicalCodeSet, List<List<Node>> inputs, int delta,
			List<Integer> queryEntities) {
		
		if (result.size() >= associationLimits) {
			return true;
		}
		double limits = 1;
		for (List<Node> eachIndexList : inputs)
			limits *= eachIndexList.size();
		if (limits == 0)
			return false;
		for (double i = 0; i < limits; i++) {
			List<Node> eachLE = new ArrayList<>(inputs.size());
			double iteratorDivider = 1;
			for (int j = 0; j < inputs.size(); j++) {
				int index = (int) ((i % (iteratorDivider * inputs.get(j).size())) / iteratorDivider);
				eachLE.add(inputs.get(j).get(index));
				iteratorDivider *= inputs.get(j).size();
			}

			// each possible valid association.
			SimpleGraph<Integer, IntegerEdge> graphX = new SimpleGraph<>(IntegerEdge.class);
			for (Node indexNode : eachLE) {// for each endpoint's path
				if (!graphX.containsVertex(indexNode.id))
					graphX.addVertex(indexNode.id);
				Node nextNode = indexNode.father;
				while (nextNode.id != -1) {
					if (!graphX.containsVertex(indexNode.id))
						graphX.addVertex(indexNode.id);
					if (!graphX.containsVertex(nextNode.id))
						graphX.addVertex(nextNode.id);

					if (indexNode.relation > 0)
						graphX.addEdge(nextNode.id, indexNode.id,
								new IntegerEdge(nextNode.id, indexNode.id, indexNode.relation));
					else
						graphX.addEdge(nextNode.id, indexNode.id,
								new IntegerEdge(indexNode.id, nextNode.id, -indexNode.relation));

					indexNode = nextNode;
					nextNode = nextNode.father;
				}
			}
			AssociationTree treeX = AssociationTree.newInstance(timeCounter, graphX, delta, queryEntities);
			long dulp = System.currentTimeMillis();
			if (treeX == null || canonicalCodeSet.contains(treeX.getCanonicalCode())) {
				timeCounter.constructTime += System.currentTimeMillis() - dulp;
				continue;
			}
			canonicalCodeSet.add(treeX.getCanonicalCode());
			result.add(treeX);
			timeCounter.constructTime += System.currentTimeMillis() - dulp;

			if (result.size() >= associationLimits) {
				return true;
			}

		}
		return false;
	}

}
