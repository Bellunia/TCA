package MLNs.Controller;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.XSD;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class Validator {

	private final static Logger logger = LogManager.getLogger(Validator.class);
	
	/**
	 * @param base
	 * @param input
	 * @param enableFwc
	 * @param enableOnt
	 */
	public static void run(String base, String[] input, boolean enableFwc, boolean enableOnt) {
		
		String outputFile = enableFwc ? "model.nt" : "model-fwc.nt";
		
		final FileOutputStream output;
		try {
			output = new FileOutputStream(base + "/" + outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		final StreamRDF writer = StreamRDFWriter.getWriterStream(output, Lang.NT);		
		
		StreamRDF dataStream = new StreamRDF() {

			@Override
			public void start() {
				writer.start();
			}

			@Override
			public void quad(Quad quad) {}

			@Override
			public void base(String base) {}

			@Override
			public void prefix(String prefix, String iri) {}

			@Override
			public void finish() {
				writer.finish();
			}
			
			@Override
			public void triple(Triple triple) {
				Node node = triple.getObject();
				if(node.isLiteral()) {
					if(!node.getLiteral().isWellFormed()) {
						// known issue: fix gYear literals
						if(node.getLiteralDatatypeURI() != null) {
							if(node.getLiteralDatatypeURI().equals(XSD.gYear.getURI()) || 
									node.getLiteralDatatypeURI().equals(XSD.gYear.getLocalName())) {
								Node newNode = NodeFactory.createLiteral(
										node.getLiteral().toString().substring(0, 4) + "^^" + XSD.gYear);
								triple = new Triple(triple.getSubject(), triple.getPredicate(), 
										newNode);
								logger.warn("Bad-formed literal: "+node+" - Using: "+newNode);
							}
						}
					}
				}
				writer.triple(triple);
			}
			
		};
		
		if(enableOnt) {
			String inputFile = base + "/model-tmp.nt";
			RDFDataMgr.parse(dataStream, inputFile);
			
			new File(inputFile).delete();
		} else {
			for(String path : input)
				RDFDataMgr.parse(dataStream, path);
		}
		
		if(!enableFwc)
			new File(base + "/model.nt").delete();
	}
	
	
	@SuppressWarnings("unused")
	private static void validate(String in, String out) {
		
		final FileOutputStream output;
		try {
			output = new FileOutputStream(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		final StreamRDF writer = StreamRDFWriter.getWriterStream(output, Lang.NT);		
		
		StreamRDF dataStream = new StreamRDF() {

			@Override
			public void start() {
				writer.start();
			}

			@Override
			public void quad(Quad quad) {}

			@Override
			public void base(String base) {}

			@Override
			public void prefix(String prefix, String iri) {}

			@Override
			public void finish() {
				writer.finish();
			}
			
			@Override
			public void triple(Triple triple) {
				Node node = triple.getObject();
				if(node.isLiteral()) {
					if(!node.getLiteral().isWellFormed()) {
						// known issue: fix gYear literals
						if(node.getLiteralDatatypeURI() != null) {
							if(node.getLiteralDatatypeURI().equals(XSD.gYear.getURI()) || 
									node.getLiteralDatatypeURI().equals(XSD.gYear.getLocalName())) {
								Node newNode = NodeFactory.createLiteral(
										node.getLiteral().toString().substring(0, 4) + "^^" + XSD.gYear);
								triple = new Triple(triple.getSubject(), triple.getPredicate(), 
										newNode);
//								System.out.println("Bad-formed literal: "+node+" - Using: "+newNode);
							}
						}
					}
				}
				writer.triple(triple);
			}
			
		};

		RDFDataMgr.parse(dataStream, in);
		
	}
	

}
