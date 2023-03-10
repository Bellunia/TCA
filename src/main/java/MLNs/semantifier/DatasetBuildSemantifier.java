package MLNs.semantifier;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import MLNs.util.DataIO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class DatasetBuildSemantifier {
	
	private int N_EXAMPLES;
	
	public DatasetBuildSemantifier(int numEx) {
		this.N_EXAMPLES = numEx;
	}

	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		
		int n = 100;

//		new DatasetBuildSemantifier(n).linkedACM();
		new DatasetBuildSemantifier(n).mapping();
		new DatasetBuildSemantifier(n).linkedDBLP();

	}

	public void linkedDBLP() throws FileNotFoundException {
		
		Model m = ModelFactory.createDefaultModel();
		TreeSet<String> nonwantedURIs = new TreeSet<>();
		TreeSet<String> goodURIs = new TreeSet<>();
		TreeSet<String> neighbours = new TreeSet<>();
		
		// for each publication, add CBD to model
		Scanner in = new Scanner(new File(Commons.DBLP_ACM_FIXED_CSV));
		in.nextLine();
		int i = 0;
		while (in.hasNextLine()) {
			String dblpID = in.nextLine().split(",")[0];
			String uri = Commons.DBLPL3S_NAMESPACE + dblpID.replaceAll("\"", "");
			goodURIs.add(uri);
			Model m1 = getCBD(uri, Commons.DBLPL3S_ENDPOINT, Commons.DBLPL3S_GRAPH);
			m.add(m1);
			TreeSet<String> neigh = getNeighbours(uri, m1);
			neighbours.addAll(neigh);
			System.out.println("URI        = " + uri);
			System.out.println("CBD size   = " + m1.size());
			System.out.println("Model size = " + m.size());
			System.out.println("URI Neighb = " + neigh.size());
			System.out.println("Tot Neighb = " + neighbours.size());
			// TODO remove me!
			if (++i == N_EXAMPLES)
				break;
		}
		in.close();
		
		// for each neighbour, add CBD to model as long as it's not part of a
		// publication CBD
		for (String uri : neighbours) {
			Model m1 = getCBD(uri, Commons.DBLPL3S_ENDPOINT, Commons.DBLPL3S_GRAPH);
			Resource s = ResourceFactory.createResource(uri);
			// probably the following is useless, since two publications are
			// never directly connected
			if (m1.contains(s, Commons.RDF_TYPE, Commons.DBLPL3S_PUBLICATION_CLASS)) {
				// nothing
			} else {
				// add the model if subject isn't a publication
				m.add(m1);
			}
			System.out.println("Neighb URI = " + uri);
			System.out.println("N CBD size = " + m1.size());
			System.out.println("Model size = " + m.size());
		}
		
		// collect non-wanted URIs from SPARQL query
		for(long page = 0; true; page++) {
			long offset = page * 1048576;
			System.out.println("page = "+page+" | offset = "+offset);
			ResultSet rs = Commons.sparql("SELECT DISTINCT ?s WHERE { { ?s a <"
				+ Commons.DBLPL3S_PUBLICATION_CLASS + "> } UNION { ?s a <"
				+ Commons.DBLPL3S_AUTHOR_CLASS + "> } } LIMIT 1048576 OFFSET " + offset, Commons.DBLPL3S_ENDPOINT,
				Commons.DBLPL3S_GRAPH);
			TreeSet<String> cache = new TreeSet<>();
			while (rs.hasNext())
				cache.add(rs.next().getResource("s").getURI());
			System.out.println("cache size = "+cache.size());
			if(cache.isEmpty())
				break;
			nonwantedURIs.addAll(cache);
			System.out.println("non-wanted URIs = "+nonwantedURIs.size());
		}
		System.out.println("Total URIs = " + nonwantedURIs.size());
		System.out.println("Good URIs = " + goodURIs.size());
		nonwantedURIs.removeAll(goodURIs);
		System.out.println("Non-wanted URIs = " + nonwantedURIs.size());

		// remove all triples containing non-wanted URIs
		for (String uri : nonwantedURIs) {
			Resource s = ResourceFactory.createResource(uri);
			m.removeAll(s, null, null);
			m.removeAll(null, null, s);
		}
		
		System.out.println("Model size after removal = " + m.size());
		
		System.out.println("Saving model...");
		Commons.save(m, Commons.DBLPL3S_NT);
		
	}

	/**
	 * @deprecated Use DatasetBuildSatellites to build LinkedACM.
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@Deprecated
	public void linkedACM() throws ClassNotFoundException, IOException {

		Model m = ModelFactory.createDefaultModel();
		TreeSet<String> nonwantedURIs = new TreeSet<>();
		TreeSet<String> goodURIs = new TreeSet<>();
		TreeSet<String> neighbours = new TreeSet<>();

		// for each publication, add CBD to model
		Scanner in = new Scanner(new File(Commons.DBLP_ACM_FIXED_CSV));
		in.nextLine();
		int i = 0;
		while (in.hasNextLine()) {
			String acmID = in.nextLine().split(",")[1];
			String uri = Commons.ACMRKB_NAMESPACE + acmID.replaceAll("\"", "");
			goodURIs.add(uri);
			Model m1 = getCBD(uri, Commons.ACMRKB_ENDPOINT, Commons.ACMRKB_GRAPH);
			m.add(m1);
			TreeSet<String> neigh = getNeighbours(uri, m1);
			neighbours.addAll(neigh);
			System.out.println("URI        = " + uri);
			System.out.println("CBD size   = " + m1.size());
			System.out.println("Model size = " + m.size());
			System.out.println("URI Neighb = " + neigh.size());
			System.out.println("Tot Neighb = " + neighbours.size());
			// TODO remove me!
			if (++i == N_EXAMPLES)
				break;
		}
		in.close();

		// for each neighbour, add CBD to model as long as it's not part of a
		// publication CBD
		for (String uri : neighbours) {
			Model m1 = getCBD(uri, Commons.ACMRKB_ENDPOINT, Commons.ACMRKB_GRAPH);
			Resource s = ResourceFactory.createResource(uri);
			// probably the following is useless, since two publications are
			// never directly connected
			if (m1.contains(s, Commons.RDF_TYPE, Commons.ACMRKB_PUBLICATION_CLASS)) {
				// nothing
			} else {
				// add the model if subject isn't a publication
				m.add(m1);
			}
			System.out.println("Neighb URI = " + uri);
			System.out.println("N CBD size = " + m1.size());
			System.out.println("Model size = " + m.size());
		}

		// collect non-wanted URIs from SPARQL query
		for(long page = 0; true; page++) {
			long offset = page * 1048576;
			System.out.println("page = "+page+" | offset = "+offset);
			ResultSet rs = Commons.sparql("SELECT DISTINCT ?s WHERE { { ?s a <"
					+ Commons.ACMRKB_PUBLICATION_CLASS + "> } UNION { ?s a <"
					+ Commons.ACMRKB_AUTHOR_CLASS + "> } } LIMIT 1048576 OFFSET " + offset, Commons.ACMRKB_ENDPOINT,
					Commons.ACMRKB_GRAPH);
			TreeSet<String> cache = new TreeSet<>();
			while (rs.hasNext())
				cache.add(rs.next().getResource("s").getURI());
			System.out.println("cache size = "+cache.size());
			if(cache.isEmpty())
				break;
			nonwantedURIs.addAll(cache);
			System.out.println("non-wanted URIs = "+nonwantedURIs.size());
		}
		System.out.println("Total URIs = " + nonwantedURIs.size());
		System.out.println("Good URIs = " + goodURIs.size());
		nonwantedURIs.removeAll(goodURIs);
		System.out.println("Non-wanted URIs = " + nonwantedURIs.size());

		// remove all triples containing non-wanted URIs
		for (String uri : nonwantedURIs) {
			Resource s = ResourceFactory.createResource(uri);
			m.removeAll(s, null, null);
			m.removeAll(null, null, s);
		}
		
		System.out.println("Model size after removal = " + m.size());

		// build reverse map
		HashMap<String, ArrayList<String>> map = DataIO
				.readMap(Commons.AUTHORS_SAMEAS_MAP);
		HashMap<String, String> old2new = new HashMap<>();
		for (String key : map.keySet())
			for (String val : map.get(key))
				old2new.put(val, key);
		System.out.println("# old author URIs = " + old2new.size());
		System.out.println("# new author URIs = " + map.size());

		// handle old ACM namespace
		HashMap<String, String> toNewURI = handleOldACMNamespace(m);

		// replace occurrences of authors
		Iterator<Statement> it = m.listStatements();
		Model m2 = ModelFactory.createDefaultModel();
		while (it.hasNext()) {
			Statement st = it.next();
			String sub = st.getSubject().getURI();
			if (old2new.containsKey(sub)) {
				Resource newSub = ResourceFactory.createResource(old2new
						.get(sub));
				System.out.println(sub + " -> " + newSub.getURI());
				m2.add(m2.createStatement(newSub, st.getPredicate(),
						st.getObject()));
				it.remove();
			} else {
				if(toNewURI.containsKey(sub)) {
					Resource newSub = ResourceFactory.createResource(toNewURI
							.get(sub));
					System.out.println(sub + " -> " + newSub.getURI());
					m2.add(m2.createStatement(newSub, st.getPredicate(),
							st.getObject()));
					it.remove();
				}
			}
			if (st.getObject().isURIResource()) {
				String obj = st.getObject().asResource().getURI();
				if (old2new.containsKey(obj)) {
					Resource newObj = ResourceFactory.createResource(old2new
							.get(obj));
					System.out.println(obj + " -> " + newObj.getURI());
					m2.add(m2.createStatement(st.getSubject(),
							st.getPredicate(), newObj));
					it.remove();
				} else {
					if(toNewURI.containsKey(obj)) {
						Resource newObj = ResourceFactory.createResource(toNewURI
								.get(obj));
						System.out.println(obj + " -> " + newObj.getURI());
						m2.add(m2.createStatement(st.getSubject(),
								st.getPredicate(), newObj));
						it.remove();
					}
				}
			}
		}
		m.add(m2);

		System.out.println("Saving model...");
		Commons.save(m, Commons.LINKEDACM_NT);

	}

	private HashMap<String, String> handleOldACMNamespace(Model m) {

		HashMap<String, String> oldUriToName = new HashMap<>();

		// get author names for the URIs
		Iterator<Statement> it = m.listStatements(null, Commons.FULL_NAME,
				(String) null);
		while (it.hasNext()) {
			Statement st = it.next();
			if (st.getSubject().getURI().startsWith(Commons.OLD_AUTHOR_PREFIX))
				oldUriToName.put(st.getSubject().getURI(),
						Commons.LINKEDACM_NAMESPACE
								+ "authors/"
								+ st.getObject().asLiteral().getString()
										.replaceAll("[^A-Za-z0-9]", ""));
		}
		System.out.println(oldUriToName);

		return oldUriToName;
	}

	public void mapping() throws ClassNotFoundException, IOException {

		// index elements with pubs and authors
		ArrayList<Elements> pubsAuthsL3s = DataIO
				.readList(Commons.PUBS_WITH_AUTHORS_MAP);
		HashMap<String, Elements> elemMap = new HashMap<>();
		for (Elements el : pubsAuthsL3s)
			elemMap.put(el.getURI(), el);

		PrintWriter pw = new PrintWriter(new File(Commons.DBLPL3S_LINKEDACM_NT));
		// for each publication, add CBD to model
		Scanner in = new Scanner(new File(Commons.DBLP_ACM_FIXED_CSV));
		in.nextLine();
		int i = 0;
		while (in.hasNextLine()) {
			String[] line = in.nextLine().split(",");
			String l3s = Commons.DBLPL3S_NAMESPACE
					+ line[0].replaceAll("\"", "");
			String lACM = Commons.ACMRKB_NAMESPACE
					+ line[1].replaceAll("\"", "");

			// add publication sameAs links
			pw.write("<" + l3s + "> <" + Commons.OWL_SAMEAS + "> <" + lACM
					+ "> .\n");

			for (String author : elemMap.get(l3s).getElements())
				// add author sameAs links
				pw.write("<" + author + "> <" + Commons.OWL_SAMEAS + "> <"
						+ toLinkedACMURI(author) + "> .\n");
			// TODO remove me!
			if (++i == N_EXAMPLES)
				break;
		}
		in.close();
		pw.close();

		System.out.println("Done mapping.");

	}

	private String toLinkedACMURI(String author) {
		return Commons.LINKEDACM_NAMESPACE + author.substring(32);
	}

	private Model getCBD(String uri, String endpoint, String graph) {

		String query = "DESCRIBE <" + uri + ">";
		System.out.println(query);
		Query sparqlQuery = QueryFactory.create(query, Syntax.syntaxARQ);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(
				endpoint, sparqlQuery, graph);
		Model m2;
		try {
			m2 = qexec.execDescribe();
		} catch (Exception e) {
			// the result vector is too large: create empty model
			m2 = ModelFactory.createDefaultModel();
		}
		return m2;
		
	}

	private TreeSet<String> getNeighbours(String uri, Model m1) {

		TreeSet<String> neighbours = new TreeSet<>();

		Iterator<Statement> it = m1.listStatements();
		while (it.hasNext()) {
			Statement st = it.next();
			if (st.getSubject().getURI().equals(uri)) {
				if (st.getObject().isURIResource())
					neighbours.add(st.getObject().asResource().getURI());
			}
			if (st.getObject().toString().equals(uri))
				neighbours.add(st.getSubject().getURI());
		}

		return neighbours;
	}

}
