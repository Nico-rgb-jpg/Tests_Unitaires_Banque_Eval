package fr.esgi.banque.service.impl;

import fr.esgi.banque.service.CompteRepository;
import fr.esgi.banque.service.NotificationService;
import fr.esgi.banque.service.Virement;
import fr.esgi.banque.service.CompteIntrouvableException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import fr.esgi.banque.business.Compte;

public class VirementServiceImpl {

    private final CompteRepository repository;
	private final NotificationService notifications;
	private final Clock horloge;

    public VirementServiceImpl(CompteRepository repository, NotificationService notifications, Clock horloge) {
        this.repository = repository;
        this.notifications = notifications;
        this.horloge = horloge;
    }

    public Virement virer(String numeroSource, String numeroDestination, BigDecimal montant) {
        if (montant == null|| montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant du virement doit etre positif chef");
        }
        
        if (Objects.equals(numeroSource, numeroDestination)) {
	        throw new IllegalArgumentException("Un virement vers le même compte est interdit");
        }

        repository.trouver(numeroSource)
            .orElseThrow(() -> new CompteIntrouvableException(numeroSource));
            
        repository.trouver(numeroDestination)
            .orElseThrow(() -> new CompteIntrouvableException(numeroDestination));

        Compte source = repository.trouver(numeroSource)
            .orElseThrow(() -> new CompteIntrouvableException(numeroSource));
        Compte destination = repository.trouver(numeroDestination)
            .orElseThrow(() -> new CompteIntrouvableException(numeroDestination));
        source.debiter(montant);
        destination.crediter(montant);
        repository.sauvegarder(source);
        repository.sauvegarder(destination);

        if (montant.compareTo(new BigDecimal("10000")) > 0) {
            notifications.alerter(source.getTitulaire(), "Virement de " + montant + " € depuis le compte " + numeroSource);
        }

        return new Virement(numeroSource, numeroDestination, montant, LocalDate.now(horloge));
    }
}
