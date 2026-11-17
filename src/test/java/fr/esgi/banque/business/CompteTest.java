package fr.esgi.banque.business;

import fr.esgi.banque.Compte;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class CompteTest {
	private static Compte compte(String solde, String decouvert) {
		return new Compte("FR001", "Moi", new BigDecimal(solde), new BigDecimal(decouvert));
	}

	@Test
	void shouldIncreaseBalanceWhenCrediting() {
		Compte compte = new Compte("FR001", "Moi", new BigDecimal("100.00"), new BigDecimal("200"));
		compte.crediter(new BigDecimal("50.00"));

		assertThat(compte.getSolde()).isEqualByComparingTo("150.00");
	}


	@Nested
	@DisplayName("C1 - Num et Titulaire obligés")
	class C1_NuméroEtTitulaire {
		@Test
		void compteValideConserveSesInfos() {
			BigDecimal solde = new BigDecimal("100.00");
			BigDecimal decouvert = new BigDecimal("200");
			Compte compte = new Compte("FR001", "Moi", solde, decouvert);

			assertThat(compte.getNumero()).isEqualTo("FR001");
			assertThat(compte.getTitulaire()).isEqualTo("Moi");

	assertThat(compte.getSolde()).isEqualByComparingTo("100.00");
	assertThat(compte.getDecouvertAutorise()).isEqualByComparingTo("200");
		}

		@ParameterizedTest(name = "numero [{0}] refusé")
		@NullAndEmptySource
		@ValueSource(strings = {" ", "  ", "\t"})
		void numeroNullouVideRefuse(String numero) {
			BigDecimal solde = new BigDecimal("100");
			BigDecimal decouvert = new BigDecimal("200");

			assertThatThrownBy(() -> new Compte(numero, "Moi", solde, decouvert))
	.isInstanceOf(IllegalArgumentException.class);
		}

		@ParameterizedTest(name = "titulaire [{0}] refusé")
		@NullAndEmptySource
		@ValueSource(strings = {" ", "  ", "\t"})
		void titulaireNullouVideRefuse(String titulaire) {
			BigDecimal solde = new BigDecimal("100");
			BigDecimal decouvert = new BigDecimal("200");

			assertThatThrownBy(() -> new Compte("FR001", titulaire, solde, decouvert))
	.isInstanceOf(IllegalArgumentException.class);
		}

		@Nested
		@DisplayName("C2 - Solde début et découverte obligés, découverte >= 0")
		class C2_SoldeEtDecouvert {

			@Test
			void soldeInitialNullRefuse() {
				BigDecimal decouvert = new BigDecimal("200");

				assertThatThrownBy(() -> new Compte("FR001", "Moi", null, decouvert))
	.isInstanceOf(IllegalArgumentException.class);
			}

			@ParameterizedTest(name = "découvert {0} refusé")
			@ValueSource(strings = {"-0", "-0.00", "-0.01", "-200"})
			void decouvertNegatifRefuse(BigDecimal decouvert) {
				BigDecimal solde = new BigDecimal("100");

				assertThatThrownBy(() -> new Compte("FR001", "Moi", solde, decouvert))
	.isInstanceOf(IllegalArgumentException.class);
			}

			@ParameterizedTest(name = "découvert {0} accepté")
			@ValueSource(strings = {"0", "0.00", "0.01", "200"})
			void decouvertPositifOuNulAccepte(BigDecimal decouvert) {
				BigDecimal solde = new BigDecimal("100");

				Compte compte = new Compte("FR001", "Moi", solde, decouvert);

				assertThat(compte.getDecouvertAutorise()).isEqualByComparingTo(decouvert);

			}

		}

		@Nested
		@DisplayName("C3 : crediter n'accepte qu'un montant strictement positif")
		class C3_Crediter {

			@ParameterizedTest(name = "crediter {0} sur 100.00 donne {1}")
			@CsvSource({
				            "0,01, 100.01",
							"50.00, 150.00",
							"1000,  1100.00"
			})
			void montantStrictementPositifAccepte(BigDecimal montant, BigDecimal soldeAttendu) {
				Compte compte = compte("1000.00", "200");

				compte.crediter(montant);

				assertThat(compte.getSolde()).isEqualByComparingTo(soldeAttendu);
			}

			@ParameterizedTest(name = "crediter {0} refusé")
			@NullSource
			@ValueSource(strings = {"0", "0.00", "-0.01", "-50"})
			void montantNullNulOuNegatifRefuse(BigDecimal montant) {
				Compte compte = compte("100.00", "200");

				assertThatThrownBy(() -> compte.crediter(montant))

				.isInstanceOf(IllegalArgumentException.class);

				assertThat(compte.getSolde()).isEqualByComparingTo("100.00");
			}
		}
	}
}