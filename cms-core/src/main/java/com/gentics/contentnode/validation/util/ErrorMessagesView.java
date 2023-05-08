/*
 * @author tobiassteiner
 * @date Jan 16, 2011
 * @version $Id: ErrorMessagesView.java,v 1.1.2.1 2011-02-10 13:43:40 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import com.gentics.contentnode.validation.validator.ValidationError;
import com.gentics.contentnode.validation.validator.ValidationMessage;
import com.gentics.contentnode.validation.validator.ValidationResult;

/**
 * A view of a collection that turns each item into a new
 * {@link ValidationError} upon being viewed. This is useful to turn a
 * collection of plain error messages into a collection of ValidationMessages
 * that can be used with a {@link ValidationResult}.

 * This implementation is immutable.
 * 
 * The methods equals() and hashcode() are not passed on to the underlying
 * collection. 
 */
public class ErrorMessagesView extends AbstractCollection<ValidationMessage> {
    
	protected final Collection<?> errors;
    
	public ErrorMessagesView(Collection<?> errors) {
		this.errors = errors;
	}

	@Override
	public Iterator<ValidationMessage> iterator() {
		return new Iterator<ValidationMessage>() {
			protected Iterator<?> iterator = errors.iterator();

			public boolean hasNext() {
				return iterator.hasNext();
			}

			public ValidationError next() {
				return new ValidationError(iterator.next());
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}
    
	@Override
	public int size() {
		return errors.size();
	}
}
