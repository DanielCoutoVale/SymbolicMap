package org.uppermodel.proto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * A formatter of words.
 * 
 * @author Daniel Couto Vale <danielvale@uni-bremen.de> 
 *
 */
public class WordFormatter {

	private final OWLOntology ontology;
	private final OWLDataFactory dataFactory;
	private final Reasoner reasoner;
	private final IRI modelIri = IRI.create("Word", "WordAsPattern");
	private final IRI classifierFormIri = IRI.create("Word", "ClassifierForm");
	private final IRI identifierFormIri = IRI.create("Word", "IdentifierForm");
	private final IRI mentionerFormIri = IRI.create("Word", "MentionerForm");
	private final IRI processFormIri = IRI.create("Word", "ProcessForm");
	private final IRI classifierIri = IRI.create("Word", "Classifier");
	private final IRI identifierIri = IRI.create("Word", "Identifier");
	private final IRI mentionerIri = IRI.create("Word", "Mentioner");
	private final IRI processIri = IRI.create("Word", "Process");
	private final OWLOntologyManager manager;
	private final HashMap<IRI, IRI> wordClassMap;

	public WordFormatter(OWLOntologyManager manager, OWLOntology ontology, Reasoner reasoner, Formation formation) {
		this.manager = manager;
		this.ontology = ontology;
		this.reasoner = reasoner;
		this.dataFactory = reasoner.getDataFactory();
		this.wordClassMap = new HashMap<IRI, IRI>();
		this.wordClassMap.put(classifierIri, classifierFormIri);
		this.wordClassMap.put(identifierIri, identifierFormIri);
		this.wordClassMap.put(mentionerIri, mentionerFormIri);
		this.wordClassMap.put(processIri, processFormIri);
	}

	public final Set<WordForm> makeWordFormSet(Word word) {
		Set<WordForm> wordFormSet = new HashSet<WordForm>();
		if (instanceOf(word, classifierIri)) {
			for (OWLClass formClass : makeFormSet(classifierFormIri)) {
				OWLIndividual formId = makeInstance(formClass);
				Set<OWLClass> formClassSet = reasoner.getSuperClasses(formClass, true).getFlattened();
				OWLIndividual modelId = makeModel();
				WordForm wordForm = new WordForm(formId, word.id, formClassSet, modelId);
				wordFormSet.add(wordForm);
			}
		}
		return wordFormSet;
	}

	private final OWLIndividual makeModel() {
		OWLClass modelClass = dataFactory.getOWLClass(modelIri);
		OWLIndividual modelId = makeInstance(modelClass);
		return modelId;
	}

	private final OWLIndividual makeInstance(OWLClass klass) {
		OWLIndividual formId = dataFactory.getOWLAnonymousIndividual();
		OWLAxiom classAssertionAxiom = dataFactory.getOWLClassAssertionAxiom(klass, formId);
		manager.addAxiom(ontology, classAssertionAxiom);
		return formId;
	}

	private final Set<OWLClass> makeFormSet(IRI formIri) {
		OWLClass classifierForm = dataFactory.getOWLClass(formIri);
		Set<OWLClass> classifierForms = reasoner.getSubClasses(classifierForm, false).getFlattened();
		return classifierForms;
	}

	private final boolean instanceOf(Word word, IRI classIri) {
		OWLClass classifier = dataFactory.getOWLClass(classIri);
		OWLAxiom classAssertionAxiom = dataFactory.getOWLClassAssertionAxiom(classifier, word.id);
		return reasoner.isEntailed(classAssertionAxiom);
	}

}
