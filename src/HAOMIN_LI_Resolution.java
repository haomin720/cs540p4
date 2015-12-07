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
		System.out.println("got here!!!!");
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
		dirtyTree = true;
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
		dirtyTree = true;
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
		//bfs recursive
		helper3(tree.getChild(0));
		dirtyTree = true;
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
		else {
			for (XML c : xml.getChildren()) {
				helper3(c);
			}
		}
	}

	//check if the xml is the leave of the tree
	private boolean isAtom(XML xml) {
		if (xml.getName().equals("and") || xml.getName().equals("or") || xml.getName().equals("not"))
			return false;
		return true;
	}

	//replace X||(Y&&Z) with (X||Y)&&(X||Z)
	public void distributeOrsOverAnds()
	{
		helper4(tree.getChild(0));
		dirtyTree = true;
	}

	private void helper4(XML xml) {
		//base case
		if (!xml.hasChildren())
			return;
		if (xml.getName().equals("or")) {
			//check both child
			XML child1 = xml.getChild(0);
			XML child2 = xml.getChild(1);
			if (child2.getName().equals("and")) {
				XML gc1 = child2.getChild(0);
				XML gc2 = child2.getChild(1);
				XML parent = xml.getParent();
				parent.addChild("and");
				parent.getChild("and").addChild("or");
				parent.getChild("and").addChild("or");
				parent.getChild("and").getChild(0).addChild(child1);
				parent.getChild("and").getChild(0).addChild(gc1);
				parent.getChild("and").getChild(1).addChild(child1);
				parent.getChild("and").getChild(1).addChild(gc2);
				parent.removeChild(xml);
				XML[] sub = new XML[4];
				sub[0] = parent.getChild("and").getChild(0).getChild(0);
				sub[1] = parent.getChild("and").getChild(0).getChild(1);
				sub[2] = parent.getChild("and").getChild(1).getChild(0);
				sub[3] = parent.getChild("and").getChild(1).getChild(1);
				for (XML c : sub){
					helper4(c);
				}
			}
			else if (child1.getName().equals("and")) {
				XML gc1 = child1.getChild(0);
				XML gc2 = child1.getChild(1);
				XML parent = xml.getParent();
				parent.addChild("and");
				parent.getChild("and").addChild("or");
				parent.getChild("and").addChild("or");
				parent.getChild("and").getChild(0).addChild(child2);
				parent.getChild("and").getChild(0).addChild(gc1);
				parent.getChild("and").getChild(1).addChild(child2);
				parent.getChild("and").getChild(1).addChild(gc2);
				parent.removeChild(xml);
				XML[] sub = new XML[4];
				sub[0] = parent.getChild("and").getChild(0).getChild(0);
				sub[1] = parent.getChild("and").getChild(0).getChild(1);
				sub[2] = parent.getChild("and").getChild(1).getChild(0);
				sub[3] = parent.getChild("and").getChild(1).getChild(1);
				for (XML c : sub){
					helper4(c);
				}
			}
			else {
				for (XML c : xml.getChildren()) {
					helper4(c);
				}
			}
		}
		else {
			for (XML c : xml.getChildren()) {
				helper4(c);
			}
		}
	}
	public void collapse()
	{
		// Clean up logic in tree in preparation for Resolution:
		// 1) Convert nested binary ands and ors into n-ary operators so
		// there is a single and-node child of the root logic-node, all of
		// the children of this and-node are or-nodes, and all of the
		// children of these or-nodes are literals: either atomic or negated

		//collapse and
		boolean hasand = false;
		if (tree.getChild("and") != null) {
			hasand = true;
			XML and = tree.getChild("and");
			ArrayList<XML> ands = new ArrayList<XML>();
			ands.add(and);
			boolean end = false;
			XML cur = and;
			//find all and node
			while (!end) {
				if (cur.getChild("and") == null) {
					end = true;
				}
				else {
					cur = cur.getChild("and");
					ands.add(cur);
				}
			}
			for (int i = ands.size()-1; i > 0; i--) {
				//remove from parent
				ands.get(i).getParent().removeChild(ands.get(i));
				//add under AND
				and.addChild(ands.get(i).getChild(0));
			}
			// add || bwtween and and literal
			for (XML c : tree.getChild("and").getChildren()) {
				String s = c.getName();
				if (!s.equals("or")){
					and.addChild("or");
					and.getChild(tree.getChildCount()-1).addChild(c);
					and.removeChild(c);
				}
			}

		}
		// collapse or 
		XML cur = tree;
		boolean hasor = false;
		if (hasand) {
			if (tree.getChild("and").getChild("or") != null) {
				cur = tree.getChild("and");
				hasor = true;
			}
		}
		else if (tree.getChild("or") != null){
			hasor = true;
			cur = tree;
		}

		if (hasor) {
			// find all or nodes for each or nodes under AND or LOGIC
			for (XML c : cur.getChildren()){
				ArrayList<XML> ors = new ArrayList<XML>();
				boolean end = false;
				cur = c;
				while (!end) {
					if (cur.getChild("or") == null) {
						end = true;
					}
					else {
						cur = cur.getChild("or");
						ors.add(cur);
					}
				}
				//collapse or nodes
				for (int i = ors.size()-1; i > 0; i--) {
					//remove from parent
					ors.get(i).getParent().removeChild(ors.get(i));
					//add under OR
					cur.addChild(ors.get(i).getChild(0));
				}
			}
		}

		// 2) Remove redundant literals from every clause, and then remove
		// redundant clauses from the tree

		// remove redundant literals
		if (hasand && hasor) {
			cur = tree.getChild("and");
			for (XML c : cur.getChildren()){
				removeRedundantLiteral(c);
			}
		}
		else if (hasand && !hasor) {
			removeRedundantLiteral(tree);
		}
		else if (!hasand && hasor) {
			for (XML c : tree.getChildren()){
				removeRedundantLiteral(c);
			}
		}

		// remove redundant clauses
		if (hasand && hasor) {
			cur = tree.getChild("and");
		}
		else if (!hasand && hasor) {
			cur = tree;
		}
		else {
			cur = null;
		}
		if (cur != null) {
			ArrayList<XML> check = new ArrayList<XML>();
			for (XML c : cur.getChildren()) {
				check.add(c);
			}
			while (!check.isEmpty()) {
				XML tmp = check.remove(0);
				if(setContainsClause(cur, tmp)) {
					//remove that node
					cur.removeChild(tmp);
				}
			}
		}

		// 3) Also remove any clauses that are always true (tautologies)
		// from your tree to help speed up resolution.
		if (cur != null) {
			for (XML c : cur.getChildren()) {
				if (clauseIsTautology(c)) {
					cur.removeChild(c);
				}
			}
		}
		dirtyTree = true;
	}	

	// helper method that remove repeated literal from clause
	private void removeRedundantLiteral(XML xml){
		ArrayList<XML> list = new ArrayList<XML>();
		for (XML c : xml.getChildren()){
			list.add(c);
		}
		while (!list.isEmpty()) {
			XML tmp = list.remove(0);
			boolean isNeg = false;
			String name = "";
			if (tmp.getName().equals("not")) {
				isNeg = true;
				name = tmp.getChild(0).getName();
			}
			else {
				name = tmp.getName();
			}
			for (XML n : list) {
				if (clauseContainsLiteral(n, name, isNeg)) {
					xml.removeChild(tmp);
					break;
				}
			}
		}
	}


	public boolean applyResolution()
	{
		// Implement resolution on the logic in tree.  New resolvents
		// should be added as children to the only and-node in tree.  This
		// method should return true when a conflict is found, otherwise it
		// should only return false after exploring all possible resolvents.
		// Note: you are welcome to leave out resolvents that are always
		// true (tautologies) to help speed up your search.
		boolean hasor = false;
		XML cur = this.tree;
		if (tree.getChild("and") != null) {
			cur = tree.getChild("and");
		}
		else {
			// return false when there is no set
			dirtyTree = true;
			return false;
		}
		if (cur.getChild("or") != null) {
			hasor = true;
		}
		//check once if there is no clause
		if (!hasor) {
			if (hasConflict(cur)) {
				dirtyTree = true;
				return true;
			}
			else {
				dirtyTree = true;
				return false;
			}
		}
		ArrayList<XML> queue = new ArrayList<XML>();
		for (XML c : cur.getChildren()) {
			queue.add(c);
		}
		//compare each node with each other
		while(!queue.isEmpty()) {
			XML tmp = queue.remove(0);
			//resolve is the list of its siblings
			ArrayList<XML> resolve = new ArrayList<XML>();
			for (XML c : cur.getChildren()) {
				resolve.add(c);
			}
			resolve.remove(tmp);
			for (XML n : resolve){
				XML resolvent = resolve(tmp, n);
				if (resolvent != null) {
					queue.add(resolvent);
					cur.addChild(resolvent);
				}
			}
			//check conflict
			if (hasConflict(cur)) {
				dirtyTree = true;
				return true;
			}
		}
		dirtyTree = true;
		return false;
	}

	public XML resolve(XML clause1, XML clause2)
	{
		// Attempt to resolve these two clauses and return the resulting
		// resolvent.  You should remove any redundant literals from this 
		// resulting resolvent.  If there is a conflict, you will simply be
		// returning an XML node with zero children.  If the two clauses cannot
		// be resolved, then return null instead.

		//ArrayList<XML> cl1 = new ArrayList<XML>();
		//ArrayList<XML> cl2 = new ArrayList<XML>();
		for (XML c : clause1.getChildren()) {
			//for all the not literal
			if (c.getName().equals("not")) {
				String name = getAtomFromLiteral(c.getChild(0));
				//compare with all the positive
				for (XML d : clause1.getChildren()) {
					//find resolution!
					if (name.equals(d.getName())) {
						clause1.addChild("or");
						XML resolvent = clause1.getChild(clause1.getChildCount()-1);
						clause1.removeChild(clause1.getChild(clause1.getChildCount()-1));
						for (XML a : clause1.getChildren()) {
							resolvent.addChild(a);
						}
						for (XML b : clause2.getChildren()) {
							resolvent.addChild(b);
						}
						resolvent.removeChild(c);
						resolvent.removeChild(d);
						return resolvent;
					}	
				}
			}
			//for all the other
			else {
				String name = getAtomFromLiteral(c.getChild(0));
				//compare with all the positive
				for (XML d : clause1.getChildren()) {
					//find resolution!
					if (d.getName().equals("not")) {
						if (name.equals(getAtomFromLiteral(d))) {
							clause1.addChild("or");
							XML resolvent = clause1.getChild(clause1.getChildCount()-1);
							clause1.removeChild(clause1.getChild(clause1.getChildCount()-1));
							for (XML a : clause1.getChildren()) {
								resolvent.addChild(a);
							}
							for (XML b : clause2.getChildren()) {
								resolvent.addChild(b);
							}
							resolvent.removeChild(c);
							resolvent.removeChild(d);
							return resolvent;
						}	
					}
				}
			}
		}
		return null;
	}	

	// this helper method determine if there is a conflic exists in the given set 
	private boolean hasConflict(XML set) {
		ArrayList<XML> atomList = new ArrayList<XML>();
		ArrayList<XML> negatomList = new ArrayList<XML>();
		for (XML c : set.getChildren()) {
			if (c.getChildCount() == 1) {
				if (c.getChild("not") != null) {
					negatomList.add(c.getChild(0));
				}
				atomList.add(c.getChild(0));
			}
		}
		for (XML n : atomList) {
			for (XML m : negatomList) {
				if (getAtomFromLiteral(n).equals(getAtomFromLiteral(m))) {
					return true;
				}
			}
		}
		return false;
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
		// Implement to return the name of the atom in this literal as a string.
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
		// Implement to return true when the set contains a clause with the
		// same set of literals as the clause parameter.  Otherwise, return false.
		for (XML xml : set.getChildren()){
			if (isIdentical(clause, xml)) {
				return true;
			}
		}
		return false;
	}
	// this helper returns clause literals as arraylist

	public boolean isIdentical(XML clause1, XML clause2) {
		if (clause1.getName().equals(clause2.getName())){
			for (XML n: clause1.getChildren()){
				for (XML m: clause2.getChildren()) {
					if (!n.getName().equals(m.getName())) {
						return false;
					}
				}
			}
		}
		else {
			return false;
		}
		return true;
	}

	public boolean clauseIsTautology(XML clause)
	{
		// Implement to return true when this clause contains a literal
		// along with the negated form of that same literal.  Otherwise, return false.
		ArrayList<String> atoms = new ArrayList<String>();
		ArrayList<String> negatoms = new ArrayList<String>();
		for (XML c : clause.getChildren()) {
			if (c.getName().equals("not")) 
				negatoms.add(c.getChild(0).getName());
			else 
				atoms.add(c.getName());
		}
		for (String s : atoms) {
			for (String c : negatoms) {
				if (s.equals(c)) {
					return true;
				}
			}
		}
		return false;
	}	

}
