package bot.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="username")
    private String username;

    @Column(name="password")
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "chatid", unique = true)
    private Long chatId;

    @Column(name = "completed_leads")
    private Integer completedLeads;

    @Column(name = "wrote_password")
    private Boolean wrotePassword;

    @Column(name = "wrote_username")
    private Boolean wroteUsername;

    @Column(name = "is_registered")
    private Boolean isRegistered;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "session_id")
    private Session session;

    @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
    private List<Lead> leads = new ArrayList<>();
}