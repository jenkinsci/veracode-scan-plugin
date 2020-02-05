package io.jenkins.plugins.veracode.data;

/**
 * This class represents a SCA Component (based on the detailed report)
 *
 */
public class SCAComponent {
    private final String name;
    private final boolean isBacklisted;
    private final boolean isNew;
    private final boolean violatedPolicy;

    public SCAComponent(String name, boolean isBacklisted, boolean isNew, boolean violatedPolicy) {
        this.name = name;
        this.isBacklisted = isBacklisted;
        this.isNew = isNew;
        this.violatedPolicy = violatedPolicy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SCAComponent other = (SCAComponent) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public String getName() {
        return name;
    }

    public boolean isBacklisted() {
        return isBacklisted;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isViolatedPolicy() {
        return violatedPolicy;
    }
}
