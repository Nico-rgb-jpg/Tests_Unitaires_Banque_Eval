package fr.esgi.banque.service.impl;

import fr.esgi.banque.service.CompteRepository;
import fr.esgi.banque.service.NotificationService;
import fr.esgi.banque.service.Virement;
import fr.esgi.banque.service.CompteIntrouvableException;
import fr.esgi.banque.service.VirementRefuseException;
import fr.esgi.banque.service.impl.VirementServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fr.esgi.banque.business.Compte;
import fr.esgi.banque.business.SoldeInsuffisantException;

@ExtendWith(MockitoExtension.class)
class VirementServiceImplTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static final Clock LUNDI = Clock.fixed(
            Instant.parse("2026-11-16T10:00:00Z"), PARIS);

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

    @Test
    void v2_virementVersLeMememCompteRefuse() {
        BigDecimal montant = new BigDecimal("50.00");

        assertThatThrownBy(() -> service.virer("FR001", "FR001", montant))
        .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).sauvegarder(any());
    }

    private static Compte compte(String numero, String titulaire, String solde) {
        return new Compte(numero, titulaire, new BigDecimal(solde), BigDecimal.ZERO);
    }

    private void comptesExistants(Compte... comptes) {
        for (Compte compte : comptes) {
            when(repository.trouver(compte.getNumero())).thenReturn(Optional.of(compte));
        }
    }

    @Test
    void v3_compteSourceIntrouvable() {
        when(repository.trouver("INCONNU")).thenReturn(Optional.empty());
        BigDecimal montant = new BigDecimal("50.00");

        assertThatThrownBy(() -> service.virer("INCONNU", "FR002", montant))
        .isInstanceOf(CompteIntrouvableException.class);
        verify(repository, never()).sauvegarder(any());
    }

    @Test
    void v3_compteDestinationIntrouvable() {
        Compte source = compte("FR001", "Moi", "500.00");
        comptesExistants(source);
        when(repository.trouver("INCONNU")).thenReturn(Optional.empty());
        BigDecimal montant = new BigDecimal("50.00");

        assertThatThrownBy(() -> service.virer("FR001", "INCONNU", montant))
        .isInstanceOf(CompteIntrouvableException.class);
        verify(repository, never()).sauvegarder(any());
        assertThat(source.getSolde()).isEqualByComparingTo("500.00");
    }
    
    @Test
    void v4_casNominalDebitCrediteSauvegarderEtDateDuJour() {
        Compte source = compte("FR001", "Moi", "500.00");
        Compte destination = compte("FR002", "Jérome", "100.00");
        comptesExistants(source, destination);

        Virement virement = service.virer("FR001", "FR002", new BigDecimal("200.00"));

        assertThat(source.getSolde()).isEqualByComparingTo("300.00");
        assertThat(destination.getSolde()).isEqualByComparingTo("300.00");
        verify(repository).sauvegarder(source);
        verify(repository).sauvegarder(destination);
        assertThat(virement.numeroSource()).isEqualTo("FR001");
        assertThat(virement.numeroDestination()).isEqualTo("FR002");
        assertThat(virement.montant()).isEqualByComparingTo("200.00");
        assertThat(virement.date()).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void v5_soldeInsuffisantRemonteEtRienNEstSauvegarde() {
        Compte source = compte("FR001", "Moi", "100.00");
        Compte destination = compte("FR002", "Jéromé", "100.00");
        comptesExistants(source, destination);
        BigDecimal montant = new BigDecimal("100.01");

        assertThatThrownBy(() -> service.virer("FR001", "FR002", montant))
        .isInstanceOf(SoldeInsuffisantException.class);
        verify(repository, never()).sauvegarder(any());
        assertThat(source.getSolde()).isEqualByComparingTo("100.00");
        assertThat(destination.getSolde()).isEqualByComparingTo("100.00");
    }

    @ParameterizedTest(name = "V6 :virement de {0} euros : alerte au titulaire source")
    @ValueSource(strings = {"10000.01", "15000"})
    void v6_alerteAuTitulaireSourceAuDeLaDe10000(BigDecimal montant) {
        comptesExistants(compte("FR001", "Moi", "50000"), compte("FR002", "Jérome", "0"));

        service.virer("FR001", "FR002", montant);

        verify(notifications).alerter(eq("Moi"), anyString());
    }

    @ParameterizedTest(name = "V6 : virement {0} euros : pas d''alerte")
    @ValueSource(strings = {"10000", "10000.00", "9999.99", "50"})
    void v6_pasDAlerteJusquA10000(BigDecimal montant) {
        comptesExistants(compte("FR001", "Moi", "50000"), compte("FR002", "Jérome", "0"));

        service.virer("FR001", "FR002", montant);

        verify(notifications, never()).alerter(anyString(), anyString());
    }

    private static final Clock SAMEDI = Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), PARIS);

    private static final Clock DIMANCHE = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), PARIS);

    @Test
    void v7_aucunVirementLeDimanche() {
        VirementServiceImpl serviceLeDimanche = new VirementServiceImpl(repository, notifications, DIMANCHE);
        Compte source = compte("FR001", "Moi", "500.00");
        Compte destination = compte("FR002", "Jéroe", "100.00");

        lenient().when(repository.trouver("FR001")).thenReturn(Optional.of(source));
        lenient().when(repository.trouver("FR002")).thenReturn(Optional.of(destination));
        BigDecimal montant = new BigDecimal("50.00");

        assertThatThrownBy(() -> serviceLeDimanche.virer("FR001", "FR002", montant))
        .isInstanceOf(VirementRefuseException.class);
        verify(repository, never()).sauvegarder(any());
        assertThat(source.getSolde()).isEqualByComparingTo("500.00");
        assertThat(destination.getSolde()).isEqualByComparingTo("100.00");
    }

    @Test
    void v7_virementAccepteLeSamedi() {
        VirementServiceImpl serviceLeSamedi = new VirementServiceImpl(repository, notifications, SAMEDI);
        Compte source = compte("FR001", "Moi", "500.00");
        Compte destination = compte("FR002", "Jéroe", "100.00");
        comptesExistants(source, destination);

        Virement virement = serviceLeSamedi.virer("FR001", "FR002", new BigDecimal("50.00"));
        
        assertThat(virement.date()).isEqualTo(LocalDate.of(2026, 11, 14));
        verify(repository).sauvegarder(source);
        verify(repository).sauvegarder(destination);
    }

}

    

    