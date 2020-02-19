package io.jenkins.plugins.veracode;

import java.util.List;

import hudson.model.ParameterValue;
import hudson.model.ParametersAction;

public class VeracodeParametersAction extends ParametersAction {

    public VeracodeParametersAction(List<ParameterValue> parameters) {
        super(parameters);
    }

    public VeracodeParametersAction(ParameterValue... parameters) {
        super(parameters);
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}