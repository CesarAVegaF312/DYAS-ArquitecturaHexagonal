package edu.unisabana.dyas.reservas.adaptadores.salida.memoria;

import edu.unisabana.dyas.reservas.aplicacion.puertos.salida.RepositorioReservas;
import edu.unisabana.dyas.reservas.dominio.Reserva;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de salida: implementa el puerto {@link RepositorioReservas} en memoria.
 *
 * <p>Sirve para desarrollar y probar sin base de datos, pero las reservas se pierden al reiniciar.
 * Se activa con el perfil {@code memoria}, que es el perfil por defecto.
 */
@Repository
@Profile("memoria")
public class RepositorioReservasEnMemoria implements RepositorioReservas {

    private final Map<UUID, Reserva> reservas = new ConcurrentHashMap<>();

    @Override
    public Reserva guardar(Reserva reserva) {
        reservas.put(reserva.getId(), reserva);
        return reserva;
    }

    @Override
    public Optional<Reserva> buscarPorId(UUID id) {
        return Optional.ofNullable(reservas.get(id));
    }

    @Override
    public List<Reserva> buscarPorSalaYFecha(String sala, LocalDate fecha) {
        return reservas.values().stream()
                .filter(r -> r.getSala().equals(sala) && r.getFecha().equals(fecha))
                .toList();
    }

    @Override
    public List<Reserva> buscarPorFecha(LocalDate fecha) {
        return reservas.values().stream()
                .filter(r -> r.getFecha().equals(fecha))
                .sorted(Comparator.comparing(Reserva::getSala).thenComparing(Reserva::getInicio))
                .toList();
    }
}
