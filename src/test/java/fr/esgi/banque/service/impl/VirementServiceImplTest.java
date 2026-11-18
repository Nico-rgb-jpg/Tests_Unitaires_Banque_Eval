package fr.esgi.banque.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import java.math.BigDecimal;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;


@ExtendWith(MockitoExtension.class)
class VirementServiceImplTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static final Clock LUNDI = Clock(Instant.parse("2026-11-16T10:00:00Z"), Paris);

    @Mock
    private CompteRepository repository;

    @Mock
    private NotificationService notifications;
    private VirementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VirementServiceImpl(repository, notifications, LUNDI);
    }

    @ParameterizedTest(name = "V1 : montant {0} refusé")
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-0.01", "-100"})
    void v1_montantNullNulOuNegatifRefuse(BigDecimal montant) {
        assertThatThrownBy(() -> service.virer("FR001", "FR002", montant))
        .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository, notifications);
    }

    

}

    

    