package nl.knaw.dans.validatebagpack.core.rules;

import nl.knaw.dans.lib.util.ruleengine.BagValidatorRule;
import nl.knaw.dans.lib.util.ruleengine.RuleResult;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestRule implements BagValidatorRule {
    @Override
    public RuleResult validate(Path bagPath) {
        if (Files.exists(bagPath)) {
            return RuleResult.ok();
        }
        else {
            return RuleResult.error("Path does not exist: " + bagPath);
        }
    }
}
