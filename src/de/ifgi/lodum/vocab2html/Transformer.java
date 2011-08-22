package de.ifgi.lodum.vocab2html;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class Transformer {

	private static String vocabURL = "http://carsten.io/osm/osm-provenance.rdf";
	private static String htmlURL = "http://observedchange.com/osp/ns";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		convertToHTML();

	}

	private static void convertToHTML() {

		// we try to build a Jena model and then serialize it:

		Model model = ModelFactory.createDefaultModel();
		try {
			model.read(vocabURL);

			// add the namespaces that we need for RDFa, in case they are not in
			// the model:
			model.setNsPrefix("dc", "http://purl.org/dc/terms/");
			model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
			model.setNsPrefix("prv", "http://purl.org/net/provenance/ns#");
			model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

			String html = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">"
					+ "<html xml:lang=\"en\"\n xmlns=\"http://www.w3.org/1999/xhtml\"\n";

			// add namespaces:
			Map<String, String> prefixes = model.getNsPrefixMap();
			Set<String> keys = prefixes.keySet();
			Iterator<String> keyIter = keys.iterator();
			while (keyIter.hasNext()) {
				String key = keyIter.next();
				html += ("xmlns:" + key + "=\"" + prefixes.get(key) + "\"\n");
			}

			html += ">\n"
					+ "<head>\n"
					+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n"
					+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />\n";

			// find title, date, version, description, creators:

			StmtIterator iter = model
					.listStatements(
							null,
							model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
							model.createResource("http://www.w3.org/2002/07/owl#Ontology"));

			RDFNode ontology = iter.toList().get(0).getSubject();
			iter = model.listStatements((Resource) ontology, model
					.createProperty("http://purl.org/dc/elements/1.1/#title"),
					(Resource) null);

			String title = iter.toList().get(0).getString();

			// date:
			iter = model.listStatements((Resource) ontology, model
					.createProperty("http://purl.org/dc/elements/1.1/#date"),
					(Resource) null);
			String date = iter.toList().get(0).getString();

			// description:
			iter = model
					.listStatements(
							(Resource) ontology,
							model.createProperty("http://purl.org/dc/elements/1.1/#description"),
							(Resource) null);
			String description = iter.toList().get(0).getString();

			// version
			iter = model
					.listStatements(
							(Resource) ontology,
							model.createProperty("http://purl.org/dc/elements/1.1/#hasVersion"),
							(Resource) null);
			String version = iter.toList().get(0).getString();

			// creators:
			iter = model
					.listStatements(
							(Resource) ontology,
							model.createProperty("http://purl.org/dc/elements/1.1/#creator"),
							(Resource) null);

			ArrayList<String> creators = new ArrayList<String>(0);
			while (iter.hasNext()) {
				RDFNode c = iter.next().getObject();
				creators.add(c.toString());
			}

			html += "<title>" + title + "</title>\n" + "</head>\n<body>\n";

			html += "<h1 about=\"\" property=\"dcterms:title\" xml:lang=\"en\">"
					+ title
					+ "</h1>\n"
					+ "<div about=\"\" typeof=\"foaf:Document\">\n";

			html += "<div property=\"rdfs:label\" content=\"Document that defines the "
					+ title + "\" xml:lang=\"en\"></div>\n";

			html += "<div rel=\"foaf:primaryTopic\">\n"
					+ "  <div about=\""
					+ vocabURL
					+ "\">\n"
					+ "    <div rel=\"rdf:type\" resource=\"http://purl.org/net/provenance/ns#DataItem\"></div>\n"
					+ "    <div rel=\"rdf:type\" resource=\"http://www.w3.org/2002/07/owl#Ontology\"></div>\n"
					+ "    <div property=\"dc:title\" content=\"" + title
					+ " namespace\" xml:lang=\"en\"></div>";

			html += "      <div rel=\"prv:createdBy\">\n"
					+ "	    <div rel=\"rdf:type\" resource=\"prv:DataCreation\"></div>\n";

			for (String creator : creators) {
				html += "<div rel=\"prv:performedBy\" resource=\"" + creator
						+ "\"></div>\n"
						+ "      <div rel=\"dcterms:creator\" resource=\""
						+ creator + "\"></div>\n";
			}

			html += "	    <div property=\"prv:performedAt\" content=\"" + date
					+ "\" datatype=\"xsd:dateTime\"></div>\n"
					+ "      </div>\n"
					+ "      <div property=\"dcterms:created\" content=\""
					+ date + "\" datatype=\"xsd:dateTime\"></div>\n"
					+ "     </div>\n" + "   </div>\n" + "</div>";

			html += "<h2>" + date + "</h2>\n" + "<dl>"
					+ " <dt>This version:</dt>" + " <dd><a href=\"" + htmlURL
					+ "-" + date + "\">" + htmlURL + "-" + date + "</a></dd>"
					+ " <dt>Latest version:</dt>" + " <dd><a href=\"" + htmlURL
					+ "\">" + htmlURL + "</a></dd>";

			html += "  <dt>Revision:</dt>\n" + " <dd>Revision: " + version
					+ "</dd>\n" + " <dt>Authors:</dt>\n";
			for (String creator : creators) {
				html += " <dd about=\"" + creator
						+ "\" typeof=\"foaf:Person\">\n"
						+ "   <a rel=\"foaf:homepage\" href=\"" + creator
						+ "\" property=\"foaf:name\">Name</a>\n"
						+ "   (Affiliation)\n" + "   </dd>\n";
			}
			html += "</dl>\n";

			html += "<p class=\"copyright\">Copyright &copy; 2011 - 2011 <em>Names</em>.</p>";

			html += "<p><a rel=\"license\" href=\"http://creativecommons.org/licenses/by/3.0/\">"
					+ "<img alt=\"Creative Commons License\" style=\"border: 0pt none; float: right;\" "
					+ "src=\"http://i.creativecommons.org/l/by/3.0/88x31.png\" /></a>"
					+ "This work is licensed under a <a href=\"http://creativecommons.org/licenses/by/3.0/\">"
					+ "Creative Commons License</a>. This copyright applies to the <em>"
					+ title + "</em> and accompanying documentation.</p>";

			html += "<p about=\"\" resource=\"http://www.w3.org/TR/rdfa-syntax\" rel=\"dcterms:conformsTo\">"
					+ "  <a href=\"http://validator.w3.org/check?uri=referer\">"
					+ "<img src=\"http://www.w3.org/Icons/valid-xhtml-rdfa.png\" "
					+ "style=\"border: 0pt none; float: right\" alt=\"Valid XHTML + RDFa\" /> </a>"
					+ "  Regarding underlying technology, the "
					+ title
					+ " relies heavily on W3C's <a href=\"http://www.w3.org/RDF/\">RDF</a> technology, "
					+ "an open Web standard that can be freely used by anyone.</p>";

			html += "  <p>This visual layout and structure of the specification was adapted from the "
					+ "<a href=\"http://rdfs.org/sioc/spec/\">SIOC Core Ontology Specification</a> edited by "
					+ "Uldis Bojars and John G. Breslin and the "
					+ "<a href=\"http://trdf.sourceforge.net/provenance/ns.html\">Provenance Vocabulary Core"
					+ " Ontology Specification</a> edited by Olaf Hartig and Jun Zhao, and "
					+ "<a href=\"http://open-biomed.sourceforge.net/opmv/ns.html\">Open Provenance Model "
					+ "Vocabulary</a> edited by Jun Zhao.</p>";

			html += "<hr />\n" + "<div>\n"
					+ "<h2 id=\"sec-abstract\">Abstract</h2>\n<p>\n"
					+ "  <span about=\"http://observedchange.com/tisc/ns#\" "
					+ "property=\"dcterms:description\">" + description
					+ "</span>\n" + "</p>";

			html += "</span>\n"
					+ "  <span about=\"\" property=\"dcterms:description\">This documents specifies the "
					+ "classes and properties introduced by the " + title
					+ ".</span>\n" + "</p>\n" + "</div>\n" + "<hr />\n"
					+ "<div class=\"status\">\n"
					+ "<h2 id=\"sec-status\">Status of this document</h2>";

			html += "<p><strong>NOTE:</strong> <em>This section describes the status of this "
					+ "document at the time of its publication. Other documents may supersede this "
					+ "document.</em></p>"
					+ "<p>This specification is an evolving document. This document may be updated or "
					+ "added to based on implementation experience, but no commitment is made by the "
					+ "authors regarding future updates. This document is generated by combining a "
					+ "machine-readable <a href=\""
					+ vocabURL
					+ "\">"
					+ title
					+ " Namespace</a> "
					+ "expressed in RDF/XML with a specification template and a set of per-term "
					+ "documents.</p>" + "</div>";

			html += "<h2 id=\"sec-toc\">Table of contents</h2>" +
					"<ol>" +
					"  <li><a href=\"#sec-intro\">Introduction</a></li>" +
					"  <li><a href=\"#sec-glance\">"+title+" at a glance</a></li>" +
					"  <li><a href=\"#sec-desc\">Description of "+title+"</a></li>" +
					"  <ul>" +
					"      <li><a href=\"#sec-extension\">3.1. Evolution and " +
					"extension of "+title+"</a></li>" +
					"  </ul>" +
					"  <li><a href=\"#sec-specification\">Cross-reference for core "+title+" " +
					"classes and properties</a></li>" +
					"  <li><a href=\"#sec-ack\">Acknowledgements</a></li>" +
					"  <li><a href=\"#sec-reference\">References</a></li>" +
					"  <li><a href=\"#sec-changes\">Change log</a></li>" +
					"</ol>" +
					"<hr />";
			
			html += "</body>\n</html>";
			// write file:
			BufferedWriter out = new BufferedWriter(
					new FileWriter("index.html"));
			out.write(html);
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
