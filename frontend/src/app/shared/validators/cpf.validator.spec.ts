import { FormControl } from '@angular/forms';
import { cpfEhValido, cpfValidator } from './cpf.validator';

describe('cpfEhValido', () => {
  it.each([
    ['12912861012', 'Catharyna'],
    ['09506382000', 'Cleuddônio'],
    ['85733854057', 'Catianna'],
    ['58872160006', 'Cutardo'],
    ['76179646090', 'Coândrya'],
  ])('aceita o CPF real do seed %s (%s)', (cpf) => {
    expect(cpfEhValido(cpf)).toBe(true);
  });

  it('rejeita um CPF com o primeiro dígito verificador trocado', () => {
    // último par de dígitos de um CPF válido do seed, com o primeiro DV incorreto
    expect(cpfEhValido('12912861022')).toBe(false);
  });

  it('rejeita um CPF com o segundo dígito verificador trocado', () => {
    expect(cpfEhValido('12912861019')).toBe(false);
  });

  it.each(['00000000000', '11111111111', '22222222222', '99999999999'])(
    'rejeita %s mesmo que a conta do dígito verificador feche — regra explícita do CPF',
    (cpf) => {
      expect(cpfEhValido(cpf)).toBe(false);
    },
  );

  it('rejeita string com menos de 11 dígitos', () => {
    expect(cpfEhValido('1291286101')).toBe(false);
  });

  it('rejeita string com mais de 11 dígitos', () => {
    expect(cpfEhValido('129128610120')).toBe(false);
  });

  it('rejeita string vazia', () => {
    expect(cpfEhValido('')).toBe(false);
  });
});

describe('cpfValidator', () => {
  it('não reclama de um controle vazio — "required" é responsabilidade de outro validador', () => {
    const controle = new FormControl('');

    expect(cpfValidator(controle)).toBeNull();
  });

  it('aceita CPF válido formatado com máscara (pontos e traço)', () => {
    const controle = new FormControl('129.128.610-12');

    expect(cpfValidator(controle)).toBeNull();
  });

  it('aceita CPF válido sem máscara', () => {
    const controle = new FormControl('12912861012');

    expect(cpfValidator(controle)).toBeNull();
  });

  it('devolve { cpf: true } para um CPF com dígito verificador errado', () => {
    const controle = new FormControl('129.128.610-13');

    expect(cpfValidator(controle)).toEqual({ cpf: true });
  });

  it('devolve { cpf: true } para entrada parcial, ainda sendo digitada', () => {
    const controle = new FormControl('129.128');

    expect(cpfValidator(controle)).toEqual({ cpf: true });
  });

  it('trata valor não-string (ex.: null) como vazio, sem lançar erro', () => {
    const controle = new FormControl<string | null>(null);

    expect(cpfValidator(controle)).toBeNull();
  });
});
