package br.com.bantads.msauth.reboot;

import br.com.bantads.msauth.saga.ComandoProcessadoRepository;
import br.com.bantads.msauth.usuario.SeedRunner;
import br.com.bantads.msauth.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class RebootService {

    private final UsuarioRepository usuarioRepository;
    private final ComandoProcessadoRepository comandoProcessadoRepository;
    private final SeedRunner seedRunner;

    public RebootService(
            UsuarioRepository usuarioRepository,
            ComandoProcessadoRepository comandoProcessadoRepository,
            SeedRunner seedRunner) {
        this.usuarioRepository = usuarioRepository;
        this.comandoProcessadoRepository = comandoProcessadoRepository;
        this.seedRunner = seedRunner;
    }

    public synchronized int reboot() {
        usuarioRepository.deleteAll();
        comandoProcessadoRepository.deleteAll();
        return seedRunner.popular();
    }
}
