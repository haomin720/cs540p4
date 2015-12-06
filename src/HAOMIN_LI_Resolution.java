import java.util.ArrayList;

import processing.core.PApplet;
import processing.data.XML;

public class HAOMIN_LI_Resolution extends DrawableTree
{
	public HAOMIN_LI_Resolution(PApplet p, XML tree) 
	{ 
		super(p); 
		this.tree = tree; 
		dirtyTree = true;
	}

	public void eliminateBiconditions()
	{

		// do a BFS to traverse through the tree
		ArrayList<XML> queue = new ArrayList<XML>();
		XML[] children = tree.getChildren();
		for (XML xml : children) {
			queue.add(xml);
		}
		while (!queue.isEmpty()) {
			XML cur = queue.remove(0);
			if (cur.getName().equals("bicondition")) {
				ArrayList<XML> tmp = new ArrayList<XML>();
				tmp = helper1(cur);
				for (XML xml : tmp) {
					queue.add(xml);
				}
			}
			if (cur.hasChildren()) {
				XML[] kids = cur.getChildren();
				for (XML c : kids) {
					queue.add(c);
				}
			}
		}
	}
	private ArrayList<XML> helper1(XML xml) {
		XML parent = xml.getParent();
		XML child1 = xml.getChild(0);
		XML child2 = xml.getChild(1);
		//create new logic
		XML and = parent.addChild("and");
		and.addChild("condition");
		and.addChild("condition");
		XML cond1 = and.getChild(0);
		XML cond2 = and.getChild(1);
		cond1.addChild(child1);
		cond1.addChild(child2);
		cond2.addChild(child2);
		cond2.addChild(child1);
		//remove bicondition
		XML bicond = parent.getChild("bicondition");
		parent.removeChild(bicond);
		//return condition's children
		ArrayList<XML> children = new ArrayList<XML>();
		for (XML c : cond1.getChildren()) {
			children.add(c);
		}
		for (XML c : cond2.getChildren()) {
			children.add(c);
		}
		return children;
	}

	public void eliminateConditions()
	{
		// add first logic into the queue
		ArrayList<XML> queue = new ArrayList<XML>();
		XML[] children = tree.getChildren();
		for (XML xml : children) {
			queue.add(xml);
		}
		// BFS all logic nodes
		while (!queue.isEmpty()) {
			XML cur = queue.remove(0);
			if (cur.getName().equals("condition")) {
				ArrayList<XML> tmp = new ArrayList<XML>();
				tmp = helper2(cur);
				for (XML xml : tmp) {
					queue.add(xml);
				}
			}
			else if (cur.hasChildren()) {
				XML[] kids = cur.getChildren();
				for (XML c : kids) {
					queue.add(c);
				}
			}
		}
	}

	private ArrayList<XML> helper2(XML xml) {
		XML parent = xml.getParent();
		XML child1 = xml.getChild(0);
		XML child2 = xml.getChild(1);
		parent.addChild("or");
		XML or = parent.getChild("or");
		or.addChild("not");
		or.addChild(child2);
		or.getChild("not").addChild(child1);
		ArrayList<XML> children = new ArrayList<XML>();
		children.add(child1);
		children.add(child2);
		return children;

	}

	public void moveNegationInwards()
	{
		//find all the negation that apply to a logic, using BFS
		//ArrayList<XML> neg = new ArrayList<XML>();
		//ArrayList<XML> or = new ArrayList<XML>();
		//ArrayList<XML> and = new ArrayList<XML>();

		/*
		 * while (!queue.isEmpty()) {
			XML cur = queue.remove(0);
			else {
				if (cur.hasChildren()) {
					for (XML c : cur.getChildren()) {
						queue.add(c);
					}
				}	
			}
		}
		 * */
		helper3(tree.getChild(0));
	}

	private void helper3 (XML xml) {
		//base case
		if (isAtom(xml)) {
			return;
		}
		if (xml.getName().equals("not")) {
			// base case
			if (isAtom(xml.getChild(0)))
				return;
			// replace !!X with X
			if (xml.getChild("not") != null) {
				//XML tmp1 = xml.getChild("not");
				XML parent1 = xml.getParent();
				parent1.addChild(xml.getChild("not").getChild(0));
				parent1.removeChild(xml);
				if (!isAtom(parent1.getChild(0))) {
					helper3(parent1.getChild(0));
				}
				else
					return;
			}
			// replace !(X&&Y) with !X||!Y
			else if (xml.getChild("or") != null) {
				XML child1 = xml.getChild(0).getChild(0);
				XML child2 = xml.getChild(0).getChild(1);
				XML parent2 = xml.getParent();
				parent2.addChild("or");
				parent2.getChild("or").addChild("not");
				parent2.getChild("or").addChild("not");
				parent2.getChild("or").getChild(0).addChild(child1);
				parent2.getChild("or").getChild(1).addChild(child2);
				parent2.removeChild(xml);
				if (isAtom(child1) && isAtom(child2)) {
					return;
				}
				else{
					if(!isAtom(child1))
						helper3(child1);
					if(!isAtom(child2))
						helper3(child2);
				}	
			}
			// replace  !(X||Y) with !X && !Y
			else if (xml.getChild("and") != null) {
				XML child1 = xml.getChild(0).getChild(0);
				XML child2 = xml.getChild(0).getChild(1);
				XML parent3 = xml.getParent();
				parent3.addChild("and");
				parent3.getChild("and").addChild("not");
				parent3.getChild("and").addChild("not");
				parent3.getChild("and").getChild(0).addChild(child1);
				parent3.getChild("and").getChild(1).addChild(child2);
				parent3.removeChild(xml);
				if (isAtom(child1) && isAtom(child2)) {
					return;
				}
				else{
					if(!isAtom(child1))
						helper3(child1);
					if(!isAtom(child2))
						helper3(child2);
				}	
			}
			else {
				for (XML c : xml.getChildren()) {
					helper3(c);
				}
			}
			
		}
	}
	
	//check if the xml is the leave of the tree
	private boolean isAtom(XML xml) {
		if (xml.getName().equals("and") || xml.getName().equals("or") || xml.getName().equals("not"))
			return false;
		return true;
	}
	
	//replace
	public void distributeOrsOverAnds()
	{
		

	}

	public void collapse()
	{
		// TODO - Clean up logic in tree in preparation for Resolution:
		// 1) Convert nested binary ands and ors into n-ary operators so
		// there is a single and-node child of the root logic-node, all of
		// the children of this and-node are or-nodes, and all of the
		// children of these or-nodes are literals: either atomic or negated	
		// 2) Remove redundant literals from every clause, and then remove
		// redundant clauses from the tree.
		// 3) Also remove any clauses that are always true (tautologies)
		// from your tree to help speed up resolution.
	}	

	public boolean applyResolution()
	{
		// TODO - Implement resolution on the logic in tree.  New resolvents
		// should be added as children to the only and-node in tree.  This
		// method should return true when a conflict is found, otherwise it
		// should only return false after exploring all possible resolvents.
		// Note: you are welcome to leave out resolvents that are always
		// true (tautologies) to help speed up your search.
		return false;
	}

	public XML resolve(XML clause1, XML clause2)
	{
		// TODO - Attempt to resolve these two clauses and return the resulting
		// resolvent.  You should remove any redundant literals from this 
		// resulting resolvent.  If there is a conflict, you will simply be
		// returning an XML node with zero children.  If the two clauses cannot
		// be resolved, then return null instead.
		return null;
	}	

	// REQUIRED HELPERS: may be helpful to implement these before collapse(), applyResolution(), and resolve()
	// Some terminology reminders regarding the following methods:
	// atom: a single named proposition with no children independent of whether it is negated
	// literal: either an atom-node containing a name, or a not-node with that atom as a child
	// clause: an or-node, all the children of which are literals
	// set: an and-node, all the children of which are clauses (disjunctions)

	public boolean isLiteralNegated(XML literal) 
	{ 
		// Implement to return true when this literal is negated and false otherwise.
		if (literal.getName().equals("not")) {
			return true;
		}
		return false; 
	}

	public String getAtomFromLiteral(XML literal) 
	{ 
		// TODO - Implement to return the name of the atom in this literal as a string.
		if (isLiteralNegated(literal)){
			return literal.getChild(0).getName();
		}
		return literal.getChild(0).getName();
	}	

	public boolean clauseContainsLiteral(XML clause, String atom, boolean isNegated)
	{
		// Implement to return true when the provided clause contains a literal
		// with the atomic name and negation (isNegated).  Otherwise, return false.
		if (isNegated) {
			for (XML xml : clause.getChildren("not")) {
				if (xml.getName().equals(atom)) {
					return true;
				}
			}
		}
		else {
			for (XML xml : clause.getChildren()) {
				if (!isLiteralNegated(xml)) {
					if (getAtomFromLiteral(xml).equals(atom)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean setContainsClause(XML set, XML clause)
	{
		// TODO - Implement to return true when the set contains a clause with the
		// same set of literals as the clause parameter.  Otherwise, return false.
		return false;
	}

	public boolean clauseIsTautology(XML clause)
	{
		// TODO - Implement to return true when this clause contains a literal
		// along with the negated form of that same literal.  Otherwise, return false.
		return false;
	}	

}
