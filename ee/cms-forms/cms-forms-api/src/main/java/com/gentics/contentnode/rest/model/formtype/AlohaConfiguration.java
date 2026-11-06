package com.gentics.contentnode.rest.model.formtype;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Settings for the aloha rich-content editor within form-grid.
 */
public class AlohaConfiguration implements Serializable {
    @Serial
    private static final long serialVersionUID = -3046022360522557384L;

    /**
     * The list of plugins to be used in the aloha text field.
     */
    @JsonProperty
    public List<String> plugins;

    /**
     * The settings which will be set to the Aloha context, as `Aloha.settings`.
     */
    @JsonProperty
    public Map<String, Object> settings;
}
