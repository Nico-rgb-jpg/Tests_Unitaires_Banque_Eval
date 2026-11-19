package fr.esgi.banque.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Virement(String nmeroSource, String numeroDestination, BigDecimal montant, LocalDate date) {
}
