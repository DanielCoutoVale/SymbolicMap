package org.uppermodel.proto;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;

/**
 * A word form in a descriptive format.
 * 
 * @author Daniel Couto Vale <danielvale@uni-bremen.de>
 */
public class WordForm {

	public OWLIndividual id;
	public OWLIndividual word;
	public Set<OWLClass> classSet;
	public OWLIndividual model;

	public WordForm(OWLIndividual id, OWLIndividual word, Set<OWLClass> classSet, OWLIndividual model) {
		this.id = id;
		this.word = word;
		this.classSet = classSet;
		this.model = model;
	}

}
