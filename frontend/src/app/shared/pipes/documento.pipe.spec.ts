import { DocumentoPipe } from './documento.pipe';

describe('DocumentoPipe', () => {
  let pipe: DocumentoPipe;

  beforeEach(() => {
    pipe = new DocumentoPipe();
  });

  describe('cpf', () => {
    it('formata 11 dígitos crus com pontos e traço', () => {
      expect(pipe.transform('12912861012', 'cpf')).toBe('129.128.610-12');
    });

    it('reformata um CPF que já chega com máscara', () => {
      expect(pipe.transform('129.128.610-12', 'cpf')).toBe('129.128.610-12');
    });

    it('devolve o valor original quando não tem 11 dígitos', () => {
      expect(pipe.transform('123', 'cpf')).toBe('123');
    });

    it('devolve o valor original para string vazia', () => {
      expect(pipe.transform('', 'cpf')).toBe('');
    });
  });

  describe('telefone', () => {
    it('formata celular de 11 dígitos (DDD + 9 dígitos)', () => {
      expect(pipe.transform('41999990001', 'telefone')).toBe('(41) 99999-0001');
    });

    it('formata fixo de 10 dígitos (DDD + 8 dígitos)', () => {
      expect(pipe.transform('4199990001', 'telefone')).toBe('(41) 9999-0001');
    });

    it('devolve o valor original quando não tem 10 nem 11 dígitos', () => {
      expect(pipe.transform('123', 'telefone')).toBe('123');
    });

    it('ignora a máscara já existente e reformata do zero', () => {
      expect(pipe.transform('(41) 99999-0001', 'telefone')).toBe('(41) 99999-0001');
    });
  });
});
