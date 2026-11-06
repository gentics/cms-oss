package com.gentics.contentnode.rest.model.formtype;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface of a condition
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
    @JsonSubTypes.Type(ConditionOr.class),
    @JsonSubTypes.Type(ConditionAnd.class),
    @JsonSubTypes.Type(ConditionNot.class),
    @JsonSubTypes.Type(ConditionEquals.class),
    @JsonSubTypes.Type(ConditionMatches.class),
    @JsonSubTypes.Type(ConditionContains.class),
    @JsonSubTypes.Type(ConditionIncludedIn.class),
    @JsonSubTypes.Type(ConditionBiggerThan.class),
    @JsonSubTypes.Type(ConditionLowerThan.class),
    @JsonSubTypes.Type(ConditionEmpty.class),
    @JsonSubTypes.Type(ConditionVisible.class),
    @JsonSubTypes.Type(ConditionDisabled.class),
})
public interface Condition extends Serializable {
}

/** Met when one or more of the provided conditions are met. */
class ConditionOr implements Condition {
	private static final long serialVersionUID = 7030491204595673888L;

	@JsonProperty("or")
	public List<Condition> or;

	/**
	 * Creator
	 * @param or list of conditions
	 */
	@JsonCreator
	public ConditionOr(@JsonProperty(value = "or", required = true) List<Condition> or) {
		this.or = or;
	}
}

/** Met when all provided conditions are met. */
class ConditionAnd implements Condition {
	private static final long serialVersionUID = 4043791066847319240L;

	@JsonProperty("and")
	public List<Condition> and;

	/**
	 * Creator
	 * @param and list of conditions
	 */
	@JsonCreator
	public ConditionAnd(@JsonProperty(value = "and", required = true) List<Condition> and) {
		this.and = and;
	}
}

/** Inverts the provided condition result. */
class ConditionNot implements Condition {
	private static final long serialVersionUID = -2049834243377040322L;

	@JsonProperty("not")
	public Condition not;

	/**
	 * Creator
	 * @param not negated condition
	 */
	@JsonCreator
	public ConditionNot(@JsonProperty(value = "not", required = true) Condition not) {
		this.not = not;
	}
}

/**
 * Interface for condition sources
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
	@JsonSubTypes.Type(ConditionSettingSource.class),
	@JsonSubTypes.Type(ConditionSchemaSource.class)
})
interface ConditionControlSource extends Serializable {
}

/**
 * Source targeting a setting defined on the element.
 */
class ConditionSettingSource implements ConditionControlSource {
	private static final long serialVersionUID = -1432085737191098776L;

	@JsonProperty("setting")
	public String setting;

	/**
	 * Creator
	 * @param setting name of the source setting
	 */
	@JsonCreator
	public ConditionSettingSource(@JsonProperty(value = "setting", required = true) String setting) {
		this.setting = setting;
	}
}

/**
 * Source targeting one of the defined form schema fields.
 */
class ConditionSchemaSource implements ConditionControlSource {
	private static final long serialVersionUID = -2516996442879865632L;

	@JsonProperty("schema")
	public String schema;

	/**
	 * Creator
	 * @param schema schema field
	 */
	@JsonCreator
	public ConditionSchemaSource(@JsonProperty(value = "schema", required = true) String schema) {
		this.schema = schema;
	}
}

/**
 * Met when the control value strictly equals the specified value.
 * {@code equals} is a {@link CompareValue}: String, Number, Boolean, or null.
 */
class ConditionEquals implements Condition {
	private static final long serialVersionUID = -4374678801234509303L;

	/** Source of the control value to compare. */
	@JsonProperty("source")
	public ConditionControlSource source;

	// TODO?
	@JsonProperty("container")
	public String container;

	/** Value to compare against. */
	@JsonProperty("equals")
	public Object equals;

	/**
	 * Creator
	 * @param source condition source
	 * @param equals compare value
	 */
	@JsonCreator
	public ConditionEquals(@JsonProperty(value = "source", required = true) ConditionControlSource source,
			@JsonProperty(value = "equals", required = true) Object equals) {
		this.source = source;
		this.equals = equals;
	}
}

/** Met when the specified RegExp matches the control value. */
class ConditionMatches implements Condition {
	private static final long serialVersionUID = 9134872860932051956L;

	@JsonProperty("source")
	public ConditionControlSource source;

	/** RegExp to match against the control value. */
	@JsonProperty("matches")
	public String matches;

	/**
	 * Creator
	 * @param source condition source
	 * @param matches regex to match against
	 */
	@JsonCreator
	public ConditionMatches(@JsonProperty(value = "source", required = true) ConditionControlSource source,
			@JsonProperty(value = "matches", required = true) String matches) {
		this.source = source;
		this.matches = matches;
	}
}

/** Met when one of the control's array values equals the specified value. */
class ConditionContains implements Condition {
	private static final long serialVersionUID = -1554919484116290029L;

	@JsonProperty("source")
	public ConditionControlSource source;

	@JsonProperty("contains")
	public Object contains;

	/**
	 * Creator
	 * @param source condition source
	 * @param contains compare value
	 */
	@JsonCreator
	public ConditionContains(@JsonProperty(value = "source", required = true) ConditionControlSource source,
			@JsonProperty(value = "contains", required = true) Object contains) {
		this.source = source;
		this.contains = contains;
	}
}

/** Met when the control's value equals one of the specified values. */
class ConditionIncludedIn implements Condition {
	private static final long serialVersionUID = 744693779594941469L;

	@JsonProperty("source")
	public ConditionControlSource source;

	@JsonProperty("includedIn")
	public List<Object> includedIn;

	/**
	 * Creator
	 * @param source condition source
	 * @param includedIn compare values
	 */
	@JsonCreator
	public ConditionIncludedIn(@JsonProperty(value = "source", required = true) ConditionControlSource source,
			@JsonProperty(value = "includedIn", required = true) List<Object> includedIn) {
		this.source = source;
		this.includedIn = includedIn;
	}
}

/**
 * Met when the control value is (exclusively) bigger than the specified value.
 */
class ConditionBiggerThan implements Condition {
	private static final long serialVersionUID = 5478864275544540226L;

	@JsonProperty("source")
	public ConditionControlSource source;

	@JsonProperty("biggerThan")
	public Double biggerThan;

	/**
	 * Creator
	 * @param source condition source
	 * @param biggerThan compare value
	 */
	@JsonCreator
	public ConditionBiggerThan(@JsonProperty(value = "source", required = true) ConditionControlSource source,
			@JsonProperty(value = "biggerThan", required = true) Double biggerThan) {
		this.source = source;
		this.biggerThan = biggerThan;
	}
}

/**
 * Met when the control value is (exclusively) lower than the specified value.
 */
class ConditionLowerThan implements Condition {
	private static final long serialVersionUID = -5889645926584651648L;

	@JsonProperty("source")
	public ConditionControlSource source;

	@JsonProperty("lowerThan")
	public Double lowerThan;

	/**
	 * Creator
	 * @param source condition source
	 * @param lowerThan compare value
	 */
	@JsonCreator
	public ConditionLowerThan(@JsonProperty(value = "source", required = true) ConditionControlSource source,
			@JsonProperty(value = "lowerThan", required = true) Double lowerThan) {
		this.source = source;
		this.lowerThan = lowerThan;
	}
}

/** Met when the control value is or isn't empty/null. */
class ConditionEmpty implements Condition {
	private static final long serialVersionUID = -7577807569111598999L;

	@JsonProperty("source")
	public ConditionControlSource source;

	@JsonProperty("empty")
	public Boolean empty;

	/**
	 * Creator
	 * @param source condition source
	 * @param empty empty flag
	 */
	@JsonCreator
	public ConditionEmpty(@JsonProperty(value = "source", required = true) ConditionControlSource source,
			@JsonProperty(value = "empty", required = true) Boolean empty) {
		this.source = source;
		this.empty = empty;
	}
}

/** Met when the control is or isn't visible. */
class ConditionVisible implements Condition {
	private static final long serialVersionUID = 7413570163136576930L;

	@JsonProperty("source")
	public ConditionControlSource source;

	@JsonProperty("visible")
	public Boolean visible;

	/**
	 * Creator
	 * @param source condition source
	 * @param visible visible flag
	 */
	@JsonCreator
	public ConditionVisible(@JsonProperty(value = "source", required = true) ConditionControlSource source,
			@JsonProperty(value = "visible", required = true) Boolean visible) {
		this.source = source;
		this.visible = visible;
	}
}

/** Met when the control is or isn't disabled. */
class ConditionDisabled implements Condition {
	private static final long serialVersionUID = -6334676496638919197L;

	@JsonProperty("source")
	public ConditionControlSource source;

	@JsonProperty("disabled")
	public Boolean disabled;

	/**
	 * Creator
	 * @param source condition source
	 * @param disabled disabled flag
	 */
	@JsonCreator
	public ConditionDisabled(@JsonProperty(value = "source", required = true) ConditionControlSource source,
			@JsonProperty(value = "disabled", required = true) Boolean disabled) {
		this.source = source;
		this.disabled = disabled;
	}
}
