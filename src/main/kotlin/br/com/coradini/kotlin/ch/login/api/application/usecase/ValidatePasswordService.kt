package br.com.coradini.kotlin.ch.login.api.application.usecase

import br.com.coradini.kotlin.ch.login.api.application.port.input.ValidatePasswordUseCase
import br.com.coradini.kotlin.ch.login.api.domain.model.Password
import br.com.coradini.kotlin.ch.login.api.domain.model.RuleViolation
import br.com.coradini.kotlin.ch.login.api.domain.model.ValidationResult
import br.com.coradini.kotlin.ch.login.api.domain.service.PasswordValidator
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ValidatePasswordService(
    private val passwordValidator: PasswordValidator,
    meterRegistry: MeterRegistry
) : ValidatePasswordUseCase {
    private val validCounter: Counter = meterRegistry.counter(METRIC_VALIDATIONS, TAG_RESULT, RESULT_VALID)
    private val invalidCounter: Counter = meterRegistry.counter(METRIC_VALIDATIONS, TAG_RESULT, RESULT_INVALID)

    private val violationCounters: Map<RuleViolation, Counter> = RuleViolation.entries.associateWith { rule ->
        meterRegistry.counter(METRIC_VIOLATIONS, TAG_RULE, rule.name)
    }

    override fun perform(rawPassword: String): ValidationResult {
        val result = passwordValidator.validate(Password(rawPassword))
        recordOutcome(result)
        return result
    }

    private fun recordOutcome(result: ValidationResult) {
        if (result.valid) {
            validCounter.increment()
            logger.info("Password validation completed: valid=true")
        } else {
            invalidCounter.increment()
            val violatedRules = result.violations.joinToString(", ") { it.name }
            result.violations.forEach { violationCounters.getValue(it).increment() }
            logger.info(
                "Password validation completed: valid=false, violationCount={}, violatedRules=[{}]",
                result.violations.size,
                violatedRules
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ValidatePasswordService::class.java)

        const val METRIC_VALIDATIONS = "password.validations.total"
        const val METRIC_VIOLATIONS = "password.violations.total"
        const val TAG_RESULT = "result"
        const val TAG_RULE = "rule"
        const val RESULT_VALID = "valid"
        const val RESULT_INVALID = "invalid"
    }
}
