/*
 * Author: tdanford
 * Date: Apr 21, 2009
 */
package edu.mit.csail.cgs.sigma.tgraphs;

import edu.mit.csail.cgs.cgstools.tgraphs.patterns.Edge;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.Node;

public class SigmaPatterns {

}

class Gene extends Node { 
	public Gene(String name) { 
		super(name, "gene");
	}
}

class Segment extends Node { 
	public Segment(String name) { 
		super(name, "segment");
	}
}

class Sense extends Edge { 
	public Sense(String f, String t) { 
		super(f, t, "sense");
	}
	public Sense(Node n1, Node n2) { 
		this(n1.nodeName(), n2.nodeName());
	}
}

class Antisense extends Edge { 
	public Antisense(String f, String t) { 
		super(f, t, "antisense");
	}
	public Antisense(Node n1, Node n2) { 
		this(n1.nodeName(), n2.nodeName());
	}
}

class Convergent extends Edge { 
	public Convergent(String f, String t) { 
		super(f, t, "convergent");
	}
	public Convergent(Node n1, Node n2) { 
		this(n1.nodeName(), n2.nodeName());
	}
}

class Overlapping extends Edge { 
	public Overlapping(String f, String t) { 
		super(f, t, "overlaps");
	}
	public Overlapping(Node n1, Node n2) { 
		this(n1.nodeName(), n2.nodeName());
	}
}

class Divergent extends Edge { 
	public Divergent(String f, String t) { 
		super(f, t, "divergent");
	}
	public Divergent(Node n1, Node n2) { 
		this(n1.nodeName(), n2.nodeName());
	}
}

class Tandem extends Edge { 
	public Tandem(String f, String t) { 
		super(f, t, "tandem");
	}

	public Tandem(Node n1, Node n2) { 
		this(n1.nodeName(), n2.nodeName());
	}
}

class Differential extends Edge { 
	public Differential(String f, String t) { 
		super(f, t, "differential");
	}
	
	public Differential(Node n1, Node n2) { 
		this(n1.nodeName(), n2.nodeName());
	}
}
