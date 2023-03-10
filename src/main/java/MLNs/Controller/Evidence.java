package MLNs.Controller;

import gilp.utils.Rdf2tsv;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import MLNs.Controller.NameMapper.Type;
import MLNs.model.ComparableLiteral;
import MLNs.model.Cache;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.TreeSet;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class Evidence {

	private final static Logger logger = LogManager.getLogger(Evidence.class);
	
	/**
	 * @param map
//	 * @param SRC_PATH
//	 * @param TGT_PATH
//	 * @param LNK_PATH
	 * @param THR_MIN
	 * @param THR_MAX
	 * @param THR_STEP
	 */
	public static void build(final NameMapper map, final String BASE,
			final int THR_MIN, final int THR_MAX, final int THR_STEP) {

		// for similarity join
		final Cache cache = new Cache();

		final TreeSet<ComparableLiteral> setOfStrings = build(map, BASE);
		
		// call similarity join
		MLNs.Controller.SimilarityJoin.build(map, setOfStrings, cache, BASE, THR_MIN, THR_MAX,
				THR_STEP);
		
		// append model-sim-fwc.nt to model-fwc.nt
		final FileOutputStream output;
		try {
			output = new FileOutputStream(new File(BASE + "/model-sim-temp.nt"));
		} catch (FileNotFoundException e) {
			logger.fatal(e.getMessage());
			throw new RuntimeException("File " + BASE + "/model-sim-temp.nt not found!");
		}
		
		final StreamRDF writer = StreamRDFWriter.getWriterStream(output, Lang.NT);
		writer.start();
		
		StreamRDF reader = new StreamRDF() {
			
			@Override
			public void triple(Triple triple) {
				writer.triple(triple);
			}
			
			@Override
			public void start() {
			}
			
			@Override
			public void quad(Quad quad) {
			}
			
			@Override
			public void prefix(String prefix, String iri) {
			}
			
			@Override
			public void finish() {
			}
			
			@Override
			public void base(String base) {
			}
			
		};
		
		RDFDataMgr.parse(reader, BASE + "/model-fwc.nt");
		
		StreamRDF readerSim = new StreamRDF() {
			
			@Override
			public void triple(Triple triple) {
				writer.triple(triple);
				String s = triple.getSubject().getURI();
				String p = triple.getPredicate().getURI();
				
				String o = parse(triple.getObject());
				if(o == null)
					return;
//				String relName = 
						map.add(p, Type.RELATION);
//				String name1 = 
						map.add(s, Type.ENTITY);
//				String name2 = 
						map.add(o, Type.ENTITY);
				
				// XXX oddly this shall be off
//				map.addRelationship(relName, name1, name2);
			}
			
			@Override
			public void start() {
			}
			
			@Override
			public void quad(Quad quad) {
			}
			
			@Override
			public void prefix(String prefix, String iri) {
			}
			
			@Override
			public void finish() {
			}
			
			@Override
			public void base(String base) {
			}
			
		};

		RDFDataMgr.parse(readerSim, BASE + "/model-sim-fwc.nt");
		
		writer.finish();
		
		
		// delete old file, rename temp file
		new File(BASE + "/model-fwc.nt").delete();
		new File(BASE + "/model-sim-temp.nt").renameTo(new File(BASE + "/model-fwc.nt"));
		
	}

	/**
	 * @param map
	 * @param BASE
	 */
	public static final TreeSet<ComparableLiteral> build(final NameMapper map, final String BASE) {
		
		final TreeSet<ComparableLiteral> setOfStrings = new TreeSet<>();
		
		// reader implementation
		StreamRDF dataStream = new StreamRDF() {

			@Override
			public void base(String arg0) {}

			@Override
			public void finish() {}

			@Override
			public void prefix(String arg0, String arg1) {}

			@Override
			public void quad(Quad arg0) {}

			@Override
			public void start() {}

			@Override
			public void triple(Triple arg0) {
				String s = Rdf2tsv.parse(arg0.getSubject());
				String p = arg0.getPredicate().getURI();
				// TODO if (o.isBlankNode) => URIHandler
				String o = parse(arg0.getObject());
				if(o == null)
					return;

				String relName = map.add(p, Type.RELATION);
				String subjName = map.add(s, Type.ENTITY);
				String objName = map.add(o, Type.ENTITY);
				
				// now check for non-instantiations...
				if (!p.equals(RDF.type.getURI())) {
					// it is supposed that the map contains only classes
					// and instances of these classes (see Classes.build)
					// assume non-instantiated resources are entities

					// domain/range specification
					if (p.equals(RDFS.domain.getURI())) {
						subjName = map.add(s, Type.RELATION);
						// property name, target class, is domain
						map.addRelClass(subjName, objName, true);
					}
					if (p.equals(RDFS.range.getURI())) {
						subjName = map.add(s, Type.RELATION);
						// property name, target class, is range
						map.addRelClass(subjName, objName, false);
					}

					// if subject or object are not found, it means that they
					// have not been instantiated earlier (see Classes.build)
					if (subjName == null)
						// not found => instance subject, create entity
						subjName = map.add(s, Type.ENTITY);
					else {
						// create entity form for class
						if (subjName.startsWith(Type.CLASS.toString()))
							subjName = map.classToEntityForm(subjName);
						// create stable entity form for properties
						if (subjName.startsWith(Type.RELATION.toString()))
							subjName = map.relationToEntityForm(subjName);

					}
					if (objName == null)
						// not found => instance/datatype object, create entity
						objName = map.add(o, Type.ENTITY);
					else {
						// create entity form for class
						if (objName.startsWith(Type.CLASS.toString()))
							objName = map.classToEntityForm(objName);
						// create stable entity form for properties
						if (objName.startsWith(Type.RELATION.toString()))
							objName = map.relationToEntityForm(objName);
					}

					// property, subject (entity), object (entity) names
					map.addRelationship(relName, subjName, objName);

				}

				if (arg0.getObject().isLiteral()) {
					String dtURI = arg0.getObject().getLiteralDatatypeURI();

					boolean considerString;
					if (dtURI == null)
						considerString = true;
					else
						considerString = dtURI.equals(XSD.xstring.getURI());

					if (considerString) {
//						ComparableLiteral lit = new ComparableLiteral(arg0
//								.getObject().getLiteral().toString(true), arg0
//								.getObject().getLiteral().getValue().toString());
						ComparableLiteral lit = new ComparableLiteral(o, o);
						logger.trace(lit.getVal());
						setOfStrings.add(lit);
					}
					
					map.addRelationship(relName, subjName, objName);
				}

			}

		};

		RDFDataMgr.parse(dataStream, BASE + "/model-fwc.nt");
		
		return setOfStrings;
		
	}

	private static String parse(Node obj) {
		try {
			if(obj.isURI())
				return obj.getURI();
			if(obj.isLiteral())
				return obj.getLiteralValue().toString();
			if(obj.isBlank())
				return obj.getBlankNodeLabel();
		} catch(Exception e) {
			logger.warn("Cannot parse node: "+obj);
		}
		return null;
	}
}

