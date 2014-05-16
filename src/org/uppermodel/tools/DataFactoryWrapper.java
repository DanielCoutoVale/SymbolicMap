package org.uppermodel.tools;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

/**
 * A wrapper for a data factory.
 * 
 * @author Daniel Couto Vale <danielvale@uni-bremen.de>
 */
public class DataFactoryWrapper {

	/**
	 * The data factory
	 */
	private final OWLDataFactory dataFactory;

	/**
	 * The symbolic system: Eng, Deu, Man...
	 */
	private String ss;

	/**
	 * The data factory wrapper.
	 * 
	 * @param dataFactory the data factory
	 * @param ss the symbolic system
	 */
	public DataFactoryWrapper(OWLDataFactory dataFactory, String ss) {
		this.dataFactory = dataFactory;
		this.ss = ss;
	}

	public final OWLObjectProperty makeWordProperty(String fragment) {
		return dataFactory.getOWLObjectProperty(IRI
				.create("http://www.uppermodel.org/Word#" + fragment));
	}

	public final OWLObjectProperty makeWordingProperty(String fragment) {
		return dataFactory.getOWLObjectProperty(IRI
				.create("http://www.uppermodel.org/Wording#" + fragment));
	}

	public final OWLClass makeWordClass(String fragment) {
		return dataFactory.getOWLClass(IRI
				.create("http://www.uppermodel.org/Word#" + fragment));
	}

	public final OWLClass makeWordSsClass(String fragment) {
		return dataFactory.getOWLClass(IRI
				.create("http://www.uppermodel.org/Word" + ss + "#" + fragment));
	}

	public final OWLClass makeWordingClass(String fragment) {
		return dataFactory.getOWLClass(IRI
				.create("http://www.uppermodel.org/Wording#" + fragment));
	}

	public final OWLClass makeWordingSsClass(String fragment) {
		return dataFactory.getOWLClass(IRI
				.create("http://www.uppermodel.org/Wording" + ss + "#" + fragment));
	}

	public final OWLNamedIndividual makeWordItem(String fragment) {
		return dataFactory.getOWLNamedIndividual(IRI
				.create("http://www.uppermodel.org/Word#" + fragment));
	}

	public final OWLNamedIndividual makeWordSsItem(String fragment) {
		return dataFactory.getOWLNamedIndividual(IRI
				.create("http://www.uppermodel.org/Word" + ss + "#" + fragment));
	}

	public final OWLNamedIndividual makeWordingItem(String fragment) {
		return dataFactory.getOWLNamedIndividual(IRI
				.create("http://www.uppermodel.org/Wording#" + fragment));
	}

	public final OWLNamedIndividual makeWordingSsItem(String fragment) {
		return dataFactory.getOWLNamedIndividual(IRI
				.create("http://www.uppermodel.org/Wording" + ss + "#" + fragment));
	}

	public final OWLAxiom makeTypeAscription(OWLClass type, OWLNamedIndividual instance) {
		return dataFactory.getOWLClassAssertionAxiom(type, instance);
	}

	public final OWLAxiom makeItemDeclaration(OWLNamedIndividual item) {
		return dataFactory.getOWLDeclarationAxiom(item);
	}

	public final OWLAxiom makePropertyAscription(OWLObjectProperty property, OWLNamedIndividual domain, OWLNamedIndividual range) {
		return dataFactory.getOWLObjectPropertyAssertionAxiom(property, domain, range);
	}

	public final OWLAxiom makeDisjointClasses(Set<OWLClass> classSet, OWLClass newClass) {
		Set<OWLClass> newClassSet = new HashSet<OWLClass>();
		newClassSet.addAll(classSet);
		newClassSet.add(newClass);
		return dataFactory.getOWLDisjointClassesAxiom(newClassSet);
	}

	public OWLAxiom makeSubClassOf(OWLClass subClass, OWLClass superClass) {
		return dataFactory.getOWLSubClassOfAxiom(subClass, superClass);
	}
}
