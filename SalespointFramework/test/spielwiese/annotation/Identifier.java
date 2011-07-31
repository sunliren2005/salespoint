package spielwiese.annotation;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

import com.sun.org.apache.bcel.internal.generic.INSTANCEOF;

/**
 * Entity implementation class for Entity: Identifier
 *
 */

@Embeddable
@MappedSuperclass
public class Identifier implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7962905584163007827L;
	
	private String id_;

	public Identifier() {
		id_ = UUID.randomUUID().toString();
	}
	
	public String toString() {
		return id_;
	}
	
	public boolean equals(Object o) {
		if(o == null)
			return false;
		
		if(o == this)
			return true;
		
		if(o instanceof Identifier) {
			Identifier i = (Identifier) o;
			return i.id_.equals(id_);
		} else
			return false;
	}
	
	public int hashCode() {
		return id_.hashCode();
	}
   
}