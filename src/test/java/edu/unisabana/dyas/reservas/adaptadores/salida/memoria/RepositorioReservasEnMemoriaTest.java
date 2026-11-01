package edu.unisabana.dyas.reservas.adaptadores.salida.memoria;

import edu.unisabana.dyas.reservas.aplicacion.puertos.salida.RepositorioReservas;
import edu.unisabana.dyas.reservas.aplicacion.puertos.salida.RepositorioReservasContrato;

/** El adaptador en memoria cumple el contrato del puerto. */
class RepositorioReservasEnMemoriaTest extends RepositorioReservasContrato {

    @Override
    protected RepositorioReservas crearRepositorio() {
        return new RepositorioReservasEnMemoria();
    }
}
