package com.example.jpos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** A mock stands in for a collaborator, as the tutorial's tests do with a channel. */
@ExtendWith(MockitoExtension.class)
class MockitoTest {

    @Mock
    Supplier<String> responseCode;

    @Test
    void theMockAnswersAndIsVerified() {
        when(responseCode.get()).thenReturn("00");

        assertEquals("00", responseCode.get());
        verify(responseCode).get();
    }
}
