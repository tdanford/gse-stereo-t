package edu.mit.csail.cgs.sigma.expression.ewok;

import java.util.Iterator;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.utils.Closeable;

public interface ExpressionProbeGenerator extends Expander<Region,ExpressionProbe>, Closeable {
}