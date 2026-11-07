package com.github.mostafaism1.throttlebrawl.domain.shared;

import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;
import org.pragmatica.lang.utils.Causes;


/// The player's racing name: 1 to 16 characters after trimming.
public record RiderName(String value) {
    public static Result<RiderName> riderName(String raw) {
        return Verify.ensure(raw,
                             Verify.Is::present,
                             Causes.cause("Rider name is required"))
                     .map(String::trim)
                     .flatMap(trimmed -> Verify.ensure(trimmed,
                                                       Verify.Is::lenBetween,
                                                       1,
                                                       16,
                                                       Causes.cause("Rider name must be at most 16 characters")))
                     .map(RiderName::new);
    }
}
