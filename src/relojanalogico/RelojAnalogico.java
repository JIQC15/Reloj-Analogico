package relojanalogico;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.Timer;

public class RelojAnalogico extends JFrame {

    //Atributos generales
    boolean condicionSegundero = true;
    private final static int widthPanel = 500;
    private final static int heightPanel = 500;
    int numerosTotales = 12;
    int anguloInicial = -60;
    int incrementoAngulo = 30;
    
    private BufferedImage offscreenImage;
    private Timer timer;//Intento de reloj Atomico
    private JTextField txtDia;

    //Constructor
    public RelojAnalogico() {
        this.setBounds(0, 0, widthPanel, heightPanel);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        offscreenImage = new BufferedImage(widthPanel, heightPanel, BufferedImage.TYPE_INT_ARGB);

        txtDia = new JTextField();
        txtDia.setEditable(false);
        txtDia.setHorizontalAlignment(JTextField.CENTER);
        add(txtDia, BorderLayout.SOUTH);

        Timer timer = new Timer(1000, this::actualizarDia);
        timer.start();

        //Reproducir el sonido al inicio
        //reproducirSonido("tictac.wav");
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            RelojAnalogico ra = new RelojAnalogico();
            ra.setLocationRelativeTo(null);
            ra.setVisible(true);
            ra.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ra.iniciarHilos();
        });
    }

    @Override
    public void paint(Graphics g) {
        Graphics offscreenGraphics = offscreenImage.getGraphics();
        circuloGrafico(offscreenGraphics);
        marcasReloj(offscreenGraphics);
        numerosReloj(offscreenGraphics);
        offscreenGraphics.dispose();

        g.drawImage(offscreenImage, 0, 0, this);
    }

    // Método para actualizar el día en el cuadro de texto
    private void actualizarDia(ActionEvent e) {
        Calendar calendar = Calendar.getInstance();
        Date fecha = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
        String diaSemana = sdf.format(fecha);

        // Configura la apariencia del texto
        diaSemana = diaSemana.toUpperCase();
        Font font = new Font("Arial", Font.BOLD, 18); // Puedes ajustar la fuente y el tamaño
        txtDia.setFont(font);

        // Puedes ajustar el color del texto
        txtDia.setForeground(Color.BLUE);

        txtDia.setText("Día: " + diaSemana);
    }

    private void iniciarHilos() {
        //Se ha realizado un hilo propio para cada manecilla
        Thread hiloMinuto = new Thread(this::movimientoMinutero);
        Thread hiloSegundo = new Thread(this::movimientoSegundero);
        Thread hiloHora = new Thread(this::movimientoHora);

        hiloHora.start();
        hiloMinuto.start();
        hiloSegundo.start();
    }

    //DISEÑO DEL RELOJ
    public void circuloGrafico(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setPaint(new RadialGradientPaint(
                new Point2D.Double(getWidth() / 2, getHeight() / 2),
                getWidth() / 2,
                new float[]{0.0f, 1.0f},
                new Color[]{Color.RED, Color.WHITE}
        ));

        g2d.fillOval(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        int x = this.getWidth() / 8;
        int y = this.getHeight() / 8;
        g.fillOval(x, y, x * 6, y * 6);
        marcasReloj(g);
    }

    public void marcasReloj(Graphics g) {
        Font font = new Font("Arial", Font.BOLD, 16);
        g.setFont(font);
        g.setColor(Color.BLACK);

        for (int i = 0; i < 360; i += 6) {
            marcaSegundero(g, i);
        }
        for (int i = 0; i < 360; i += 30) {
            marcaHora(g, i);
        }
        numerosReloj(g);
    }

    public void numerosReloj(Graphics g) {
        Font font = new Font("Arial", Font.BOLD, 20);
        g.setFont(font);
        g.setColor(Color.BLACK);

        for (int i = 1; i <= numerosTotales; i++) {
            int angulo = anguloInicial + (i - 1) * incrementoAngulo;
            generarNumeroReloj(g, angulo, Integer.toString(i));
        }
    }

    public void generarNumeroReloj(Graphics g, int angulo, String hora) {
        Point origen = new Point(getWidth() / 2, getHeight() / 2);
        int distanciaDesdeCentro = 110; // Ajusta la distancia desde el centro del círculo
        int offset = 30; // Ajusta la distancia entre el número y el círculo exterior

        // Obtén las coordenadas del destino ajustando la distancia desde el centro
        Point destino = getSegundoPunto(origen.x, origen.y, angulo, distanciaDesdeCentro);

        // Ajusta las coordenadas para centrar mejor los números
        int deltaX = (int) (offset * Math.cos(Math.toRadians(angulo)));
        int deltaY = (int) (offset * Math.sin(Math.toRadians(angulo)));
        destino.x += deltaX;
        destino.y += deltaY;

        // Usa una sombra para los números
        g.setColor(Color.GRAY);
        g.drawString(hora, destino.x + 2, destino.y + 2);

        // Usa el color principal para los números
        g.setColor(Color.BLACK);
        g.drawString(hora, destino.x, destino.y);
    }

    private Point getSegundoPunto(int x, int y, int angulo, int distancia) {
        double radianes = Math.toRadians(angulo);
        int xDestino = x + (int) (distancia * Math.cos(radianes));
        int yDestino = y + (int) (distancia * Math.sin(radianes));
        return new Point(xDestino, yDestino);
    }

    public void marcaHora(Graphics g, int angulo) {
        Point centro = new Point(250, 250);
        int radioInterno = 165;
        Point puntoInterno = getSegundoPunto(centro.x, centro.y, angulo, radioInterno);

        // Diseño mejorado
        g.setColor(Color.RED);
        // Dibuja un círculo más grande para representar la marca de la hora
        g.fillOval(puntoInterno.x - 8, puntoInterno.y - 8, 15, 15);
    }

    public void movimientoHora() {
        Graphics g = this.getGraphics();
        while (condicionSegundero) {
            try {
                Calendar h = Calendar.getInstance();
                int hour = h.get(Calendar.HOUR);
                dibujaHora(g, (hour * 30) - 90, Color.BLACK);
                Thread.sleep(1000);
                dibujaHora(g, (hour * 30) - 90, Color.WHITE);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void dibujaHora(Graphics g, int angulo, Color color) {
        Point origen = new Point(250, 250);
        int longitudTotal = 80; // Ajusta la longitud aquí
        int grosor = 8; // Ajusta el grosor de la línea aquí

        // Obtén las coordenadas del punto final de la manecilla
        Point punta1 = getSegundoPunto(origen.x, origen.y, angulo, longitudTotal);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);

        // Configura el grosor de la línea
        g2d.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Dibuja la manecilla (línea gruesa con extremos redondeados)
        g2d.drawLine(origen.x, origen.y, punta1.x, punta1.y);
    }

    public void marcaSegundero(Graphics g, int angulo) {
        Point centro = new Point(250, 250);
        int radioExterno = 165;

        Point puntoExterno = getSegundoPunto(centro.x, centro.y, angulo, radioExterno);

        // Dibuja un punto más pequeño para representar el extremo del segundero
        g.setColor(Color.BLACK);
        g.fillOval(puntoExterno.x - 4, puntoExterno.y - 4, 8, 8);
    }

    public void movimientoSegundero() {
        Graphics g = this.getGraphics();
        while (condicionSegundero) {
            try {
                Calendar s = Calendar.getInstance();
                int seg = s.get(Calendar.SECOND);
                dibujaSegundero(g, (seg * 6) - 90, Color.RED);
                Thread.sleep(1000);

                // Limpia la pantalla antes de dibujar el siguiente segundo
                g.setColor(Color.WHITE);
                dibujaSegundero(g, (seg * 6) - 90, Color.WHITE);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void dibujaSegundero(Graphics g, int angulo, Color color) {
        Point origen = new Point(250, 250);
        int longitudTotal = 120;

        // Obtén las coordenadas del punto final del segundero
        Point punta1 = getSegundoPunto(origen.x, origen.y, angulo, longitudTotal);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);

        // Dibuja el cuerpo alargado del segundero (línea fina)
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(origen.x, origen.y, punta1.x, punta1.y);
    }

    public void movimientoMinutero() {
        Graphics g = this.getGraphics();
        while (condicionSegundero) {
            try {
                Calendar m = Calendar.getInstance();
                int min = m.get(Calendar.MINUTE);
                dibujaMinutero(g, (min * 6) - 90, Color.BLACK);
                Thread.sleep(1000);
                dibujaMinutero(g, (min * 6) - 90, Color.WHITE);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void dibujaMinutero(Graphics g, int angulo, Color color) {
        Point origen = new Point(250, 250);
        int longitudTotal = 130; // Ajusta la longitud aquí
        int grosor = 6; // Ajusta el grosor de la línea aquí

        // Obtén las coordenadas del punto final del minutero
        Point punta1 = getSegundoPunto(origen.x, origen.y, angulo, longitudTotal);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);

        // Configura el grosor de la línea
        g2d.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Dibuja el minutero (línea gruesa con extremos redondeados)
        g2d.drawLine(origen.x, origen.y, punta1.x, punta1.y);
    }

    private void reproducirSonido(String nombreSonido) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            // Imprime la ubicación del recurso para depuración
            System.out.println("Ubicación del recurso: " + classLoader.getResource("relojanalogico/" + nombreSonido));

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(classLoader.getResource("relojanalogico/" + nombreSonido));
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            // Agrega un LineListener para detectar cuando la reproducción termina
            clip.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        // Cuando la reproducción termina, reinicia el sonido
                        clip.setFramePosition(0);
                        clip.start();
                    }
                }
            });
            clip.start();
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

}
