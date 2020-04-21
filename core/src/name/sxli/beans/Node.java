package name.sxli.beans;

import java.util.ArrayList;
import java.util.Comparator;

public class Node {
	public static Comparator<Node> cmp = new Comparator<Node>() {
		@Override
		public int compare(Node o1, Node o2) {
			return o1.id - o2.id;
		}
	};
	
	public Node father = null;
	public int id;
	public ArrayList<Node> sons = new ArrayList<Node>();
	public int relation = 0;

	public Node() {
	}

	public Node(int i) {
		id = i;
	}

	public Node(int i, Node father) {
		id = i;
		this.father = father;
	}

	public Node(int id, int relate, Node father) {
		relation = relate;
		this.id = id;
		this.father = father;
	}

	public Node clone() {
		Node n = new Node(id);
		Node curr = n;
		Node thisnode = this;
		while (thisnode.id != -1) {
			curr.relation = thisnode.relation;
			curr.father = new Node(thisnode.father.id);
			curr = curr.father;
			thisnode = thisnode.father;
		}
		curr.relation = -1;
		curr.father = new Node(-1);
		return n;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(id + " (" + relation + ") ");
		Node temp = father;
		while (temp.id != -1) {
			if (temp.father.id != -1)
				result.append(temp.id + " (" + temp.relation + ") ");
			else
				result.append(temp.id);
			temp = temp.father;
		}
		return result.toString();
	}

}
