package edu.unisabana.dyas.reservas.adaptadores.entrada.web;

import edu.unisabana.dyas.reservas.aplicacion.puertos.entrada.GestionarReservas;
import edu.unisabana.dyas.reservas.aplicacion.puertos.entrada.NuevaReserva;
import edu.unisabana.dyas.reservas.dominio.Reserva;
import edu.unisabana.dyas.reservas.dominio.ReservaInvalidaException;
import edu.unisabana.dyas.reservas.dominio.ReservaNoEncontradaException;
import edu.unisabana.dyas.reservas.dominio.SalaOcupadaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del adaptador web: rutas, códigos HTTP y validación de entradas.
 * El caso de uso se reemplaza por un doble de prueba (Mockito): el adaptador no necesita el dominio real.
 */
@WebMvcTest(ReservaController.class)
class ReservaControllerTest {

    private static final LocalDate FECHA = LocalDate.now().plusDays(1);
    private static final String CUERPO = """
            {"sala":"B-201","responsable":"ana.perez@unisabana.edu.co",
             "fecha":"%s","inicio":"10:00","fin":"12:00"}""".formatted(FECHA);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionarReservas reservas;

    private static Reserva ejemplo() {
        return Reserva.nueva("B-201", "ana.perez@unisabana.edu.co", FECHA, LocalTime.of(10, 0),
                LocalTime.of(12, 0), LocalDate.now());
    }

    @Test
    void creaUnaReserva() throws Exception {
        when(reservas.crear(any(NuevaReserva.class))).thenReturn(ejemplo());

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/reservas/")))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void listaLasReservasDeUnaFecha() throws Exception {
        when(reservas.listarPorFecha(FECHA)).thenReturn(List.of(ejemplo()));

        mvc.perform(get("/api/reservas").param("fecha", FECHA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sala").value("B-201"));
    }

    @Test
    void rechazaUnCuerpoIncompletoSinLlamarAlCasoDeUso() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("{\"sala\":\"B-201\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reservas);
    }

    @Test
    void traduceLasExcepcionesDelDominioACodigosHttp() throws Exception {
        when(reservas.crear(any(NuevaReserva.class)))
                .thenThrow(new SalaOcupadaException("B-201"))
                .thenThrow(new ReservaInvalidaException("No se puede reservar en una fecha pasada"));

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La sala B-201 ya está reservada en ese horario"));

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarUnaReservaInexistenteRespondeNoEncontrada() throws Exception {
        UUID id = UUID.randomUUID();
        when(reservas.consultar(id)).thenThrow(new ReservaNoEncontradaException(id));

        mvc.perform(get("/api/reservas/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelaUnaReserva() throws Exception {
        Reserva cancelada = ejemplo();
        cancelada.cancelar();
        when(reservas.cancelar(cancelada.getId())).thenReturn(cancelada);

        mvc.perform(post("/api/reservas/{id}/cancelacion", cancelada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }
}
