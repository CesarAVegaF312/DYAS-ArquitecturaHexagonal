package edu.unisabana.dyas.reservas.aplicacion.puertos.salida;

import edu.unisabana.dyas.reservas.dominio.Reserva;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: lo que la aplicación necesita de la persistencia, sin decir cómo se implementa.
 *
 * <p>Todo adaptador que lo implemente debe pasar las pruebas de {@code RepositorioReservasContrato}.
 */
public interface RepositorioReservas {

    /** Inserta la reserva si es nueva o reemplaza la guardada con el mismo id (por ejemplo, al cancelarla). */
    Reserva guardar(Reserva reserva);

    Optional<Reserva> buscarPorId(UUID id);

    /** Reservas de una sala en una fecha, activas y canceladas. */
    List<Reserva> buscarPorSalaYFecha(String sala, LocalDate fecha);

    /** Reservas de una fecha, ordenadas por sala y hora de inicio. */
    List<Reserva> buscarPorFecha(LocalDate fecha);
}
